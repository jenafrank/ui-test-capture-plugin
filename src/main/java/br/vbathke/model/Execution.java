package br.vbathke.model;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import br.vbathke.helper.SqliteHelper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class Execution {

	private int id = 0;
	private int idJob = 0;
	private Job job;
	private String description = "";
	private String date = "";
	
	public Execution(){		
	}

	@SuppressFBWarnings("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
	public Execution(String idExec, int idJob){
		try {
			setId(Integer.parseInt(""+idExec));
			setIdJob(idJob);
	    	job = new Job(idJob);
	    	SqliteHelper conn = new SqliteHelper();
	    	JSONArray rs;
			rs = conn.query( "SELECT * FROM tb_exec where id='"+getId()+"' and id_job='"+getIdJob()+"';" );
			if(rs.size() == 0){
				save();
				rs = conn.query( "SELECT * FROM tb_exec where id='"+getId()+"' and id_job='"+getIdJob()+"';" );
			}
			JSONObject item = rs.getJSONObject(0);
	    	setId(item.getInt("id"));
	    	setIdJob(item.getInt("id_job"));
	    	setDescription(item.getString("description"));
	    	setDate(item.getString("date"));
		} catch (Exception e) {}
	}
	
	@SuppressFBWarnings("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
	public void save(){
        Statement stmt = null;
		try {
	    	int exec = 0;
	    	SqliteHelper conn = new SqliteHelper();
	    	JSONArray rs;
			rs = conn.query( "SELECT * FROM tb_exec where id='"+getId()+"' and id_job='"+getIdJob()+"';" );
			for (int i=0; i<rs.size(); i++) {
				JSONObject item = rs.getJSONObject(i);
			    if(item.getInt("id") == getId() && item.getInt("id_job") == job.getId()){
			    	exec = item.getInt("id");
			    }
			}
			if(exec == 0){
				String sql = "INSERT INTO tb_exec(id, id_job, date, description) "
	            		+ "VALUES('"+getId()+"','"+job.getId()+"','"+(new SimpleDateFormat("yyyyMMddHHmmss")).format(new java.util.Date())+"','"+getDescription()+"');";
	            conn.update(sql);
			}else{
				String sql = "update tb_exec set description='"+getDescription()+"' where id='"+exec+"'";
	            conn.update(sql);				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@SuppressFBWarnings("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
    public String consultarHistoricoExec(String stream, int streamSize) throws Exception{
    	SqliteHelper conn = new SqliteHelper();
    	JSONArray rs = conn.query( "select tr.id_exec,"
				+ "tr.test as metodo, "
				+ "te.description as execDescription, "
				+ "tr.status as status, "
				+ "tr.description as descricao, "
				+ "te.date as executionDate, "
				+ "tt.status_description as statusDescription, "
				+ "tt.behavior as behavior, "
				+ "tr.stacktrace as stacktrace, "
				+ "tt.status as classificacao, "
				+ "tt.test_class as classe "
				+ "from tb_result tr "
				+ "inner join tb_exec te on tr.id_exec=te.id "
				+ "inner join tb_job tj on te.id_job=tj.id "
				+ "inner join tb_test tt on tt.test = tr.test and tt.id_job=tj.id "
				+ "where "
				+ "tj.id='"+job.getId()+"' and id_exec='"+getId()+"'  " 
				+ ((stream.equals("true"))?"order by tr.date asc ":"order by metodo asc") 
				+ ";" );

    	ArrayList<String> arr = new ArrayList();		
		for (int i=0; i<rs.size(); i++) {
			String historico = (new Test(job.getName(), rs.getJSONObject(i).getString("metodo"))).consultarHistorico();
			rs.getJSONObject(i).accumulate("historico", historico);
			arr.add(rs.getString(i));
		}	
		
		StringBuffer buffer = new StringBuffer();
		for(String item: arr){
			buffer.append(item);
			if(!item.equals(arr.get(arr.size() -1))){
				buffer.append(",");
			}
		}
		String retorno = buffer.toString();
		if(!retorno.equals("")){
			retorno = retorno.replace(":null,", ":\"\",");
			retorno = "{\"stack\":["+retorno+"]}";
			return retorno; 
		}else{
			return "{\"stack\":[{}]}";
		}
    }
    
	@SuppressFBWarnings("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
    public String consultarQuadro(){
    	String retorno = "";
    	SqliteHelper conn = new SqliteHelper();
    	JSONArray rs;
    	String query = "select  "
				+"'success' as label, count(*) as total "
				+"from tb_result tr  "
				+"inner join tb_exec te on tr.id_exec=te.id " 
				+"inner join tb_job tj on te.id_job=tj.id  "
				+"inner join tb_test tt on tt.test=tr.test and tt.id_job='"+this.getIdJob()+"' "
				+"where  "
				+"tj.id='"+this.getIdJob()+"' and id_exec='"+this.getId()+"' and tr.status='sucesso' and tt.status!='test_fail' "
			+"union "
			+"select  "
				+"'fail' as label, count(*) as total "
				+"from tb_result tr  "
				+"inner join tb_exec te on tr.id_exec=te.id " 
				+"inner join tb_job tj on te.id_job=tj.id " 
				+"inner join tb_test tt on tt.test=tr.test and tt.id_job='"+this.getIdJob()+"' "
				+"where  "
				+"tj.id='"+this.getIdJob()+"' and id_exec='"+this.getId()+"' and tr.status='falha' and tt.status!='test_fail' "
			+"union "
			+"select  "
				+"'app_fail' as label, count(*) as total "
				+"from tb_result tr  "
				+"inner join tb_exec te on tr.id_exec=te.id  "
				+"inner join tb_job tj on te.id_job=tj.id  "
				+"inner join tb_test tt on tt.test=tr.test and tt.id_job='"+this.getIdJob()+"' "
				+"where  "
				+"tj.id='"+this.getIdJob()+"' and id_exec='"+this.getId()+"' and tt.status='app_fail' "
			+"union "
			+"select  "
				+"'test_fail' as label, count(*) as total "
				+"from tb_result tr  "
				+"inner join tb_exec te on tr.id_exec=te.id " 
				+"inner join tb_job tj on te.id_job=tj.id  "
				+"inner join tb_test tt on tt.test=tr.test and tt.id_job='"+this.getIdJob()+"' "
				+"where  "
				+"tj.id='"+this.getIdJob()+"' and id_exec='"+this.getId()+"' and tt.status='test_fail' "
			+"union "
			+"select  "
				+"'working' as label, count(*) as total "
				+"from tb_result tr  "
				+"inner join tb_exec te on tr.id_exec=te.id  "
				+"inner join tb_job tj on te.id_job=tj.id  "
				+"inner join tb_test tt on tt.test=tr.test and tt.id_job='"+this.getIdJob()+"' "
				+"where " 
				+"tj.id='"+this.getIdJob()+"' and id_exec='"+this.getId()+"' and tt.status='';";
		try {
			rs = conn.query(query);
			retorno = rs.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return retorno;
    }
    
	@SuppressFBWarnings("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
    public int consultarHistoricoExecSize(){
    	SqliteHelper conn = new SqliteHelper();
    	int buildSize = 0;
    	JSONArray rsQtd;
		try {
			rsQtd = conn.query("select count(*) as size "
					+"from tb_result tr "
					+"inner join tb_exec te on tr.id_exec=te.id "
					+"inner join tb_job tj on te.id_job=tj.id "
					+"inner join tb_test tt on tt.test = tr.test and tt.id_job=tj.id " 
					+"where " 
					+"tj.id='"+this.getIdJob()+"' and id_exec='"+this.getId()+"';");
	    	buildSize = rsQtd.getJSONObject(0).getInt("size");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return buildSize;
    }
    

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}

	public int getIdJob() {
		return idJob;
	}

	public void setIdJob(int idJob) {
		this.idJob = idJob;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}	
}
