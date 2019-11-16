package io.jenkins.plugins.nodenotification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import hudson.model.Computer;
import io.jenkins.plugins.nodenotification.NotificationNodeProperty.Entry;
import net.sf.json.JSONObject;

public class SMSPublisher {

    private static Logger LOGGER = Logger.getLogger(SMSPublisher.class.getName());
    private static String DEFAULT_PLATLOAD_RECIPIENT_KEY = "RECIPIENT";
    private static String DEFAULT_PLAYLOAD_MESSAGE_KEY = "MESSAGE";

    public void publish(String cause, Computer c, List<NotificationNodeProperty.Entry> entrys) {
        for (Entry entry : entrys) {
            if (entry.getType() == "sms") {
                for (Endpoint endpoint: NodeNotificationConfiguration.get().getEndpoints()) {
                    if (endpoint.getType() == "sms") {
                        List<String> playloads = createPlayloads(endpoint.getPlayloadTemplate(), cause, entry, c);
                        for (String playload : playloads) {
                            _publish(endpoint.getUrl(), playload);
                        }
                    }
                }
            }
        }
    }

    public String createMessage(String cause, Computer c, NotificationNodeProperty.Entry entry) {
        StringBuilder message = new StringBuilder();
        message.append("Offline Cause: " + cause);
        message.append("\nNode Name: " + c.getName());
        message.append("\nNode URL: " + c.getUrl());
        message.append("\nMessage: " + entry.getMessage());
        return message.toString();
    }

    public List<String> createRecipients(String recipients, Computer c) {
        List<String> to = new ArrayList<String>();
        Iterator<String> iter = Splitter.on(Pattern.compile("[,; ]"))
            .omitEmptyStrings()
            .trimResults()
            .split(recipients)
            .iterator();
        while (iter.hasNext()) {
            String str = iter.next();
            try {
                to.add(str);
            } catch (UnsupportedOperationException
                    | ClassCastException
                    | NullPointerException
                    | IllegalArgumentException e) {
                LOGGER.log(Level.WARNING, "Failed to add recipient " + str + " for " + c.getName(), e);
            }
        }
        return to;
    }

    public List<String> createPlayloads(String playloadTemplate, String cause, NotificationNodeProperty.Entry entry, Computer c) {
        JSONObject playloadTemplateJson = JSONObject.fromObject(playloadTemplate);
        List<String> playloads = new ArrayList<>();
        for (String recipient : createRecipients(entry.getRecipients(), c)) {
            playloadTemplateJson.put(DEFAULT_PLATLOAD_RECIPIENT_KEY, recipient);
            playloadTemplateJson.put(DEFAULT_PLAYLOAD_MESSAGE_KEY, createMessage(cause, c, entry));
            playloads.add(playloadTemplateJson.toString());
        }
        return playloads;
    }

    public void _publish(String url, String playload) {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout(5000)
            .setConnectTimeout(5000)
            .setConnectionRequestTimeout(5000)
            .build();
        try {
            HttpPost request = new HttpPost(url);
            request.setConfig(requestConfig);
            request.setHeader("Content-type", "application/json");
            StringEntity requestEntity = new StringEntity(playload, "UTF-8");
            request.setEntity(requestEntity);
            CloseableHttpResponse response = httpclient.execute(request);
            LOGGER.log(Level.INFO, "Send SMS by " + url + " " + response.getStatusLine());
        } catch (IllegalArgumentException | IOException e) {
            LOGGER.log(Level.WARNING, "Failed to send SMS", e);
        }
    }

}