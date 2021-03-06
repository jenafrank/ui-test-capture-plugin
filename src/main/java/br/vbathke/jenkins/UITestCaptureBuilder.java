package br.vbathke.jenkins;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import net.sf.json.JSONObject;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import br.vbathke.model.Execution;
import br.vbathke.model.Job;
import br.vbathke.model.Result;
import br.vbathke.model.Test;

public class UITestCaptureBuilder extends Builder{
	private final String name;

	@DataBoundConstructor
	public UITestCaptureBuilder(String name) {
		this.name = name;
	}
	
    public String getName() {
        return name;
    }	

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
		try {
			Thread t;
			t = new Thread(new Watcher(build));
			t.start();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		public DescriptorImpl() {
			load();
		}

		@Override
		public String getDisplayName() {
			return "UI Test Capture Listener";
		}

		@Override
		public boolean isApplicable(Class type) {
			return true;
		}
		
        @Override
        public boolean configure(StaplerRequest staplerRequest, JSONObject json) throws FormException {
            save();
            return true; 
        }		
	}

	public class Watcher implements Runnable {
		private AbstractBuild build;

		public Watcher(AbstractBuild pbuild) throws NoSuchAlgorithmException {
			build = pbuild;
		}

		@Override
		public void run() {
			Job job = new Job(build.getProject().getName());
			String path = "";
			while (build.isBuilding()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {}
				try {
					path = (build.getProject().getLastBuild().getWorkspace().toString()).replace("\\", "/")+job.getXmlPath();
				} catch (NullPointerException e) {}
				persistNewTestResults(build, path);
			}
		}

		public boolean persistNewTestResults(AbstractBuild build, String path){
			try{
				File directory = new File(path);
				String[] directoryContents = new String[]{};
				try{
					directoryContents = directory.list();
				}catch(Exception e){
					e.getClass();
				}
				if(directoryContents != null){
					for(String fileName: directoryContents) {
					    File temp = new File(String.valueOf(directory),fileName);
					    if(fileName.contains(".xml") && !fileName.contains(".parsed.xml")){
						    File temp2 = new File(String.valueOf(directory),fileName.replace(".xml", ".parsed.xml"));
					    	boolean statRename = temp.renameTo(temp2);
					    	if(statRename){
						    	DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
						    	Document doc = dBuilder.parse(temp2);
						    	NodeList testCases = doc.getElementsByTagName("testcase");
						    	for (int i=0; i < testCases.getLength(); i++) {
						    		String status = "passed";
						    		Node testCase = testCases.item(i);
						    		Element eElement = (Element) testCase;
							    	NodeList error = eElement.getElementsByTagName("error");
							    	NodeList failure = eElement.getElementsByTagName("failure");
							    	NodeList skipped = eElement.getElementsByTagName("skipped");
							    	if(error.getLength()>0)
							    		status = "error";
							    	if(failure.getLength()>0)
							    		status = "failure";
							    	if(skipped.getLength()>0)
							    		status = "skipped";
							    	String methodName = eElement.getAttribute("classname")+"."+eElement.getAttribute("name");
							    	String className = eElement.getAttribute("classname");
							    	persistResult(build, className, methodName, status);
						    	}
					    	}
					    }
					}
				}
			}catch(RuntimeException e){
				throw e;
				}catch(Exception e2){
			}
			return true;
		}
	}

	public void persistResult(AbstractBuild build, String classe, String metodo, String status) {
		Job job = new Job(build.getProject().getName());
		Execution exec = new Execution(build.getId(), job.getId());
		Test test = new Test(build.getProject().getName(), metodo);
		test.setIdJob(job.getId());
		test.setTest(metodo);
		test.setTestClass(classe);
		test.save();

		Result result = new Result(job.getId(), exec.getId(), test.getTest());
		result.setStatus(status);
		try {
			result.setStacktrace(FileUtils.readFileToString(new File((build.getProject().getLastBuild().getWorkspace().toString()).replace("\\", "/")+job.getXmlPath()+classe+ ".txt")));
		} catch (Exception e1) {
			try {
				result.setStacktrace(FileUtils.readFileToString(new File((build.getProject().getRootDir().getCanonicalPath()).replace("\\", "/")+"/workspace/"+job.getXmlPath()+classe+ ".txt")));
			} catch (Exception e2) {
				result.setStacktrace("");
				e2.printStackTrace();				
			}
		}
		result.save();
	}
}