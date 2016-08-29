/*
 * The MIT License
 *
 * Copyright 2016 antweiss.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.codefresh;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 *
 * @author antweiss
 */
public class CodefreshStep extends AbstractStepImpl {
    private final String cfService;
    private String cfBranch;
     private String cfComposition;
    
    @DataBoundConstructor
    public CodefreshStep(String cfService) {
        this.cfService = cfService;
    }

    public String getCfService() {
        return cfService;
    }

    @DataBoundSetter
    public void setCfBranch(String cfBranch) {
        this.cfBranch = cfBranch;
    }

    public String getCfBranch() {
        return cfBranch;
    }
    
     @DataBoundSetter
    public void setCfComposition(String cfComposition) {
        this.cfComposition = cfComposition;
    }

    public String getCfComposition() {
        return cfComposition;
    }
    
    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {
        
        public DescriptorImpl() {
            super(Execution.class);
        }
        @Override
        public String getFunctionName() {
            return "codefreshBuild";
        }
        
        @Override
        public String getDisplayName() {
            return "Run a Codefresh Build";
        }
        
        public ListBoxModel doFillCfServiceItems(@QueryParameter("cfService") String cfService) throws  IOException, MalformedURLException {
            ListBoxModel items = new ListBoxModel();
            //default to global config values if not set in step, but allow step to override all global settings
            Jenkins jenkins;
            //Jenkins.getInstance() may return null, no message sent in that case
            try {
                jenkins = Jenkins.getInstance();
            } catch (NullPointerException ne) {
                Logger.getLogger(CodefreshStep.class.getName()).log(Level.SEVERE, null, ne);
                return null;
            }
            CodefreshBuilder.DescriptorImpl cfDesc = jenkins.getDescriptorByType(CodefreshBuilder.DescriptorImpl.class);
            String cfToken = cfDesc.getCfToken().getPlainText();
            if (cfToken == null){
                throw new IOException("No Codefresh Integration Defined!!! Please configure in System Settings.");
            }
            try {
                CFApi api = new CFApi(Secret.fromString(cfToken));
                for (CFService srv: api.getServices())
                {
                    String name = srv.getName();
                    items.add(new ListBoxModel.Option(name, name, cfService.equals(name)));

                }
            }
            catch (IOException e)
            {
                    throw e;
            }
            return items;
        }
        
      }

    
    public static final class Execution extends AbstractSynchronousNonBlockingStepExecution<Boolean> {

        @Inject
        private transient CodefreshStep step;

        @StepContextParameter
        private transient Run run;

        
        @StepContextParameter
        private transient Launcher launcher;


        @StepContextParameter
        private transient TaskListener listener;
        
   
        @Override
        protected Boolean run() throws Exception {
            
            CodefreshBuilder.LaunchComposition composition = null; 
            CodefreshBuilder.SelectService service = null;
            if (step.cfComposition != null){
                composition = new CodefreshBuilder.LaunchComposition(step.cfComposition);
            }
            if (step.cfService != null){
                service = new CodefreshBuilder.SelectService(step.cfService, step.cfBranch);
            }
            else{
                
                listener.getLogger().println("No Codefresh service definition. Check your configuration");
                return false;
            }
            CodefreshBuilder builder = new CodefreshBuilder(composition,
                                                            true, service);
            return builder.performStep(run,listener);
        }
        
        private static final long serialVersionUID = 1L;
    }
}
