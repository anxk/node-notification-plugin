package io.jenkins.plugins.nodenotification;

import java.io.IOException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

public class Endpoint extends AbstractDescribableImpl<Endpoint> {

    private String type;
    private String url;
    private String playloadTemplate;

    @DataBoundConstructor
    public Endpoint(String type, String url, String playloadTemplate) {
        this.type = type;
        this.url = url;
        this.playloadTemplate = playloadTemplate;
    }

    @DataBoundSetter
    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @DataBoundSetter
    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    @DataBoundSetter
    public void setPlayloadTemplate(String playloadTemplate) {
        this.playloadTemplate = playloadTemplate;
    }

    public String getPlayloadTemplate() {
        return playloadTemplate;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Endpoint> {

        String[] types = new String[]{"sms", "email"};

        @Override
        public String getDisplayName() {
            return "";
        }

        public FormValidation doCheckPlayloadTemplate(@QueryParameter String value) throws IOException, ServletException {
            try {
                JSONObject.fromObject(value);
            } catch (JSONException e) {
                return FormValidation.error("Please input valid json string.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckUrl(@QueryParameter String value) throws IOException, ServletException {
            if (value.startsWith("http") || value.startsWith("https")) {
                return FormValidation.ok();
            }
            return FormValidation.error("Please input valid url.");
        }

        public FormValidation doCheckType(@QueryParameter String value) throws IOException, ServletException {
            if (value.equals("sms") || value.equals("email")) {
                return FormValidation.ok();
            }
            return FormValidation.error("Please input valid type, sms or email.");
        }

        public ListBoxModel doFillTypeItems() {
            ListBoxModel items = new ListBoxModel();
            for (String type : types) {
                items.add(type);
            }
            return items;
        }

    }

}
