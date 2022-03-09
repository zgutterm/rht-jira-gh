package org.acme;

import org.apache.camel.builder.RouteBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.atlassian.jira.rest.client.api.domain.Issue;

public class Routes extends RouteBuilder {
    private List<String> courseList;
    @Override
    public void configure() throws Exception {
        courseList = fetchCourseList();
        for(int i=0; i<courseList.size(); i++){
            String courseKey = courseList.get(i);
            from("jira:newIssues?jiraUrl={{jira.url}}&username={{jira.username}}&password={{jira.password}}&jql=Project=" + courseKey + "%20AND%20status=Accepted")
                .process(exchange -> {
                    log.info("*Found Issue!*");
                    Issue issue = (Issue) exchange.getIn().getBody();
                    exchange.setProperty("projectKey", issue.getProject().getKey());
                    exchange.getIn().setHeader("GitHubIssueTitle", issue.getKey());
                    exchange.getIn().setBody(issue.getSelf() + "\n" + issue.getDescription());
                    log.info("New Jira Issue: {} {} {}", issue.getKey(), issue.getSelf(), issue.getDescription());
                })
                .toD("github:createIssue?lazyStartProducer=true&repoName=${exchangeProperty[projectKey]}&repoOwner={{github.owner}}&oauthToken={{github.oauth}}");

        }

    }

    public List<String> fetchCourseList(){
        List<String> theCourseList = new ArrayList<String>();
        String fileName = "courselist.txt";
        InputStream is = Routes.class.getClassLoader().getResourceAsStream(fileName);

        try (InputStreamReader streamReader =
            new InputStreamReader(is, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(streamReader)) {

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                theCourseList.add(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return theCourseList;
    }

}
