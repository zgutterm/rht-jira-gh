package org.acme;

import org.apache.camel.builder.RouteBuilder;
import com.atlassian.jira.rest.client.api.domain.Issue;

public class Routes extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("jira:newissues?jiraUrl={{jira.url}}&username={{jira.username}}&password={{jira.password}}&jql=project={{jira.project}} AND status=Accepted")
            .process(exchange -> {
                Issue issue = (Issue) exchange.getIn().getBody();
                exchange.setProperty("projectKey", issue.getProject().getKey());
                exchange.getIn().setHeader("GitHubIssueTitle", issue.getKey());
                exchange.getIn().setBody(issue.getSelf() + "\n" + issue.getDescription());
                log.info("New Jira Issue: {} {} {}", issue.getKey(), issue.getSelf(), issue.getDescription());
            })
            .toD("github:createIssue?lazyStartProducer=true&repoName=${exchangeProperty[projectKey]}&repoOwner={{github.owner}}&oauthToken={{github.oauth}}");

    }

}
