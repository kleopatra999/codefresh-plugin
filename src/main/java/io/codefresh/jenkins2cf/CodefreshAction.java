package io.codefresh.jenkins2cf;

import hudson.model.Action;

public class CodefreshAction implements Action {


	private final String buildUrl;

	public CodefreshAction(String buildUrl) {
		this.buildUrl = buildUrl;
	}

        @Override
	public String getDisplayName() {
		return "Codefresh";
	}

        @Override
	public String getIconFileName() {
		return "/plugin/jenkins2cf/images/codefresh.png";
	}

        @Override
	public String getUrlName() {
		return buildUrl;
	}

	public String getBuildUrl() {
		return buildUrl;
	}
}

