package io.codefresh.jenkins2cf;
import hudson.Launcher;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.model.BuildBadgeAction;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import java.io.IOException;
import static java.lang.Thread.sleep;
import java.net.MalformedURLException;
import org.kohsuke.stapler.QueryParameter;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link CodefreshBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked.
 *
 * @author Kohsuke Kawaguchi
 */
public class CodefreshBuilder extends Builder {

    private final boolean launch;
    private final String cfService;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public CodefreshBuilder(String cfService, Boolean launch) {
        this.launch = launch;
        this.cfService = cfService;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     * @return 
     */
    public boolean getLaunch() {
        return launch;
    }

    public String getCfService() {
        return cfService;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

      CFProfile profile  = new CFProfile(getDescriptor().getCfUser(), getDescriptor().getCfToken());
      String serviceId = profile.getServiceIdByName(cfService);
      
      CFApi api = new CFApi(getDescriptor().getCfToken());
      String buildId = api.startBuild(serviceId);
      String progressId = api.getBuildProgress(buildId);
      String status = api.getProgressStatus(progressId);
      String progressUrl = api.getBuildUrl(progressId);
      while (status.equals("running"))
      {
          listener.getLogger().println("Codefresh build running - "+progressUrl+"\n Waiting 5 seconds...");
          Thread.sleep(5 * 1000);
          status = api.getProgressStatus(progressId);
      }
      //build.addAction(new CodefreshAction(progressUrl));
      build.addAction(new CodefreshBuildBadgeAction(progressUrl));
      switch (status) {
          case "success":
              listener.getLogger().println("Codefresh build successfull!");
              return true;
          case "error":
              listener.getLogger().println("Codefresh build failed!");
              return false;
          default:
              listener.getLogger().println("Codefresh status "+status+" unclassified.");
              return false;
      }
      
    }
     
  

 
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private String cfUser;
        private Secret cfToken;
       // private String cfService;
       // private String cfRepoName;
        private CFApi api;

     
        public FormValidation doTestConnection(@QueryParameter("cfToken") final String cfToken) throws IOException 
        {
             api = new CFApi(Secret.fromString(cfToken));
             if (api.getUser() != null) {
                return FormValidation.ok("Success");
             }
             return FormValidation.error("Couldn't connect");
        }
        
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Define Codefresh Integration";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            cfUser = formData.getString("cfUser");
            cfToken = Secret.fromString(formData.getString("cfToken"));
       //     cfService = formData.getString("cfService");

            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        
        public String getCfUser() {
            return cfUser;
        }

//         public String getCfService() {
//            return cfService;
//        }
        
//        public String getCfRepoName() {
//            return cfRepoName;
//        }
        public Secret getCfToken() {
            return cfToken;
        }
        
        public ListBoxModel doFillCfServiceItems(@QueryParameter("cfService") String cfService) throws  IOException, MalformedURLException {
            ListBoxModel items = new ListBoxModel();
         //   CFProfile profile = new CFProfile(cfUser,cfToken,cfRepoName);
            try {
                api = new CFApi(cfToken);
                for (CFService srv: api.getServices())
                {
                    String name = srv.getName();
                    items.add(new Option(name, name, cfService.equals(name)));

                }
            }
            catch (IOException e)
            {
                    throw e;
            }
            return items;
        }

    }
    
    public static class CodefreshBuildBadgeAction implements BuildBadgeAction {

        private final String buildUrl;

        public CodefreshBuildBadgeAction(String buildUrl) {
            super();
            this.buildUrl = buildUrl;
        }

        @Override
        public String getDisplayName() {
            return "Codefresh Build Page";
        }

        @Override
        public String getIconFileName() {
            return "/plugin/jenkins2cf/images/codefresh.png";
        }

        @Override
        public String getUrlName() {
            return buildUrl;
        }

    }

}
