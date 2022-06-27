package com.bjit.common.rest.app.service.salseforce.utilities;

import com.bjit.common.rest.app.service.utilities.BusinessObjectUtility;
import com.bjit.ewc18x.utils.PropertyReader;
import com.bjit.ex.integration.model.webservice.Item;
import com.bjit.ex.integration.transfer.resultsender.ResponseResultSender;
import com.bjit.ex.integration.transfer.service.impl.LN.Constants;
import com.bjit.ex.integration.transfer.service.impl.LN.ResponseResult;
import com.bjit.ex.integration.transfer.util.ApplicationProperties;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MqlUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.Signature;
import matrix.db.SignatureList;
import matrix.util.MatrixException;

/**
 *
 * @author BJIT
 */
public class EmailSender {

    private static final org.apache.log4j.Logger RESULT_SENDER_LOGGER = org.apache.log4j.Logger.getLogger(ResponseResultSender.class);

    public static String emailSender = ApplicationProperties.getProprtyValue(Constants.emailSenderKey);

    public static String transferSuccessfulMessageHeader = ApplicationProperties.getProprtyValue(Constants.nightlyUpdateMessageHeaderKey);
    public static String transferErrorMessageHeader = ApplicationProperties.getProprtyValue(Constants.errorMessageHeaderKey);
    public static String transferUnrecognizedMessageHeader = ApplicationProperties.getProprtyValue(Constants.unrecognizedMessageHeaderKey);

    Set<String> uniqueItems = new LinkedHashSet<>();
    List<ResponseResult> uniqueItemsResponseResults = new ArrayList<>();

    /**
     * Prepares email body
     *
     * @param results is the List of ResponseResult which contains the
     * information about the item to generate email body
     * @return a StringBuilder to return prepared email body
     */
    public StringBuilder getEmailBody(List<ResponseResult> results, String tableHeader) {

        boolean successfullItemMessageFound = false;
        boolean failedItemMessageFound = false;
        boolean unrecognizedMessageFound = false;

//        The items released by the CA approver need to be unique. SO checking and tracking it here.
        for (ResponseResult responseResult : results) {
            String item = responseResult.getItem();
            if (!uniqueItems.contains(item)) {
                uniqueItems.add(item);
                uniqueItemsResponseResults.add(responseResult);
            }
        }

        StringBuilder successfulItemsTable = new StringBuilder("<table border=\"1\">");
        successfulItemsTable.append("<thead>\n"
                + "	<tr>\n"
                + "	  <th align=\"center\">Item Name</th>\n"
                + "	  <th align=\"center\">REV</th>\n"
                + "	  <th align=\"center\">" + tableHeader + "</th>\n"
                + "	</tr>\n"
                + "</thead>").append("<tbody>");

        StringBuilder failedItemsTable = new StringBuilder("<table border=\"1\">");
        failedItemsTable.append("<thead>\n"
                + "	<tr>\n"
                + "	  <th align=\"center\">Item Name</th>\n"
                + "	  <th align=\"center\">REV</th>\n"
                + "	  <th align=\"center\">" + tableHeader + "</th>\n"
                + "	</tr>\n"
                + "</thead>").append("<tbody>");

        StringBuilder unrecognizedMessageParagraphs = new StringBuilder("<div>");

        int unrecognizedMessageCounter = 1;

//        Prepares email body table for each unique item based on successful or failed item transfer
        for (ResponseResult uniqueItemsResponseResult : uniqueItemsResponseResults) {
            String itemName = uniqueItemsResponseResult.getItem();
            String resultText = uniqueItemsResponseResult.getResultText();
            String revision = uniqueItemsResponseResult.getRevision();
            String itemId = uniqueItemsResponseResult.getId();
            Boolean itemSuccess = uniqueItemsResponseResult.isSuccessful();

            if (itemName != null && !itemSuccess) {
                failedItemMessageFound = true;
                failedItemsTable.append("<tr>" + "<td><a target='_BLANK' href='" + redirectUrlGenerate(itemId) + "'>" + itemName + "</a></td><td>" + revision + "</td>" + "<td>" + resultText + "</td></tr>");
            } else if (itemName != null && itemSuccess) {
                successfullItemMessageFound = true;
                successfulItemsTable.append("<tr>" + "<td>" + itemName + "</td><td>" + revision + "</td>" + "<td>" + resultText + "</td></tr>");
            } else {
                unrecognizedMessageFound = true;
                unrecognizedMessageParagraphs.append("<p>" + unrecognizedMessageCounter + ". " + resultText + "</p>");
                unrecognizedMessageCounter++;
            }
        }

        successfulItemsTable.append("</tbody>" + "</table>");
        failedItemsTable.append("</tbody>" + "</table>");
        unrecognizedMessageParagraphs.append("</div>");

        StringBuilder emailHtmlBody = new StringBuilder("<html>");
        emailHtmlBody.append("<head>");
        emailHtmlBody.append("<style>");
        emailHtmlBody.append("#messages table {\n"
                + "  border-collapse: collapse;\n"
                + "  border: 1px solid black;\n"
                + "}\n"
                + "#messages table {\n"
                + "  border: 1px solid black;\n"
                + "}\n"
                + "#messages table td{\n"
                + "  border: 1px solid black;\n"
                + "}\n"
                + "#messages table th {\n"
                + "  border: 1px solid black;\n"
                + "}"
                + "table td{\n"
                + "  padding: 4px;\n"
                + "}");

        emailHtmlBody.append("</style>");
        emailHtmlBody.append("</head>");
        emailHtmlBody.append("<body>");
        emailHtmlBody.append("<div id='messages'>");

//        Preparing finally email body according to their status
        if (successfullItemMessageFound) {
            emailHtmlBody.append("<p>");
            emailHtmlBody.append(transferSuccessfulMessageHeader);
            emailHtmlBody.append("</p>");
            emailHtmlBody.append(successfulItemsTable);
        }

        if (failedItemMessageFound) {
            emailHtmlBody.append("<p>");
            emailHtmlBody.append(transferErrorMessageHeader);
            emailHtmlBody.append("</p>");
            emailHtmlBody.append(failedItemsTable);
        }

        if (unrecognizedMessageFound) {
//            emailIsNecessary = true;
            emailHtmlBody.append("<p>");
            emailHtmlBody.append(transferUnrecognizedMessageHeader);
            emailHtmlBody.append("</p>");
            emailHtmlBody.append(unrecognizedMessageParagraphs);
        }

        emailHtmlBody.append("</div>");

        emailHtmlBody.append("</body>");
        emailHtmlBody.append("</html>");

        return emailHtmlBody;
    }

    public String enoviaHost() {
        //return "vm3dxspace.plm.valmet.com"; // Enovia HOST 
        return PropertyReader.getProperty("generic.email.enovia.host");
    }

    public String enoviaProtocol() {
        return PropertyReader.getProperty("generic.email.enovia.protocol");
    }

    public String enoviaPort() {
        return PropertyReader.getProperty("generic.email.enovia.port");
    }

    public String enovia3DPassportHost() {
        return PropertyReader.getProperty("generic.email.3dpassport.host");
    }

    public String enovia3DPassport() {
        return enovia3DPassportHost() + "/login?service=" + enoviaProtocol() + "%3A%2F%2F" + enoviaHost() + "%3A" + enoviaPort() + "%2F3dspace%2Fcommon%2FemxNavigator.jsp%3FobjectId%3D";
    }

    public String redirectUrlGenerate(String objectId) {

        return enovia3DPassport() + objectId;

    }

    /**
     * Sends email to the appropriate recipient
     *
     * @param from denotes from which mail address the email is going to be sent
     * @param emailBody is the email body that needs to be sent
     * @param recipient is the recipient's email address
     * @return Map containing successful or error sending message
     */
    public Map<String, Object> sendMail(String from, StringBuilder emailBody, String recipient, String emailSubject) {

        Map<String, Object> resultMap = new LinkedHashMap<String, Object>();
        try {
            String host = "smtp.valmet.com";
            boolean sessionDebug = false;
            Properties props = System.getProperties();

            props.put("mail.host", host);
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.from", from);
            Session session = Session.getDefaultInstance(props, null);
            session.setDebug(sessionDebug);

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
//            for (String receipent : recipients) {
//                message.addRecipient(Message.RecipientType.TO, new InternetAddress(receipent));
//            }

            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            message.setSubject(getMailSubject(emailSubject));
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(emailBody.toString(), "text/html");
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);

            RESULT_SENDER_LOGGER.info("####################################################################");
            RESULT_SENDER_LOGGER.info("######################### SENDING EMAIL ############################");
            RESULT_SENDER_LOGGER.info("######################### HOST : " + host + " ##################");
            RESULT_SENDER_LOGGER.info("######################### SENDER : " + from + " ################");
            RESULT_SENDER_LOGGER.info("######################### RECEIVER : " + recipient + " ################");
            RESULT_SENDER_LOGGER.info("######################### SUBJECT : " + message.getSubject() + " ################");
            Transport.send(message);
            RESULT_SENDER_LOGGER.info("######################   RESULT : EMAIL SENT  ######################");
            RESULT_SENDER_LOGGER.info("####################################################################");
            RESULT_SENDER_LOGGER.info("####################################################################");
            resultMap.put("sendResult", "Successfully Email Sent.");

        } catch (MessagingException ex) {
            resultMap.put("sendResult", "Failed To Send Email. Cause: " + ex.getMessage());
        }

        return resultMap;
    }

    /**
     * Prepares Mail subject and returns email Subject String
     *
     * @param subject contains the title of mail subject
     * @return prepared email subject String
     */
    private String getMailSubject(String subject) {
        StringBuilder mailSubBuilder = new StringBuilder();
        String env = ApplicationProperties.getProprtyValue("info.enovia.environment");
        if (env != null && !"".equals(env.trim())) {
            mailSubBuilder.append(env);
            mailSubBuilder.append(": ");
        }
        mailSubBuilder.append(subject);
        return mailSubBuilder.toString();
    }

    /**
     * Finds signatures to get CA approver email address based on the
     * businessObject and sets the recipient as per requirement
     *
     * @param context is required for performing MQL Query
     * @param businessObject is the object which is being processed
     * @param item holds information required to perform the action
     * @return Map containing XML file name as key and unmarshalled ItemInfo as
     * value
     */
    public List<String> initializeResultSender(Context context, BusinessObject businessObject, Item item) throws MatrixException {
        List<String> recipient = new ArrayList();
        BusinessObjectUtility businessobjectUtil = new BusinessObjectUtility();
        String promoteUserName = "";
        String toState = item.getAttributes().get("state-to");
        String fromState = item.getAttributes().get("state-from");
        String changeId = "";
        changeId = businessobjectUtil.getChangeIdFromHistory(businessObject, context);
        SignatureList signatures = null;
        try{
        signatures = businessObject.getSignatures(context, fromState, toState);
        }
        catch(Exception e){
            
        }

        Signature promoteSignature = null;
        if (signatures != null && signatures.size() >= 1) {
            promoteSignature = signatures.get(signatures.size() - 1);
            promoteUserName = promoteSignature.getSigner();
        }

        if (promoteUserName != null && !"".equals(promoteUserName)) {
            recipient.add(getEmailAddress(context, promoteUserName));
        } else if (!changeId.equalsIgnoreCase("")) {
            List<String> users = businessobjectUtil.getUserInformation(context, changeId);
            for (String user : users) {
                recipient.add(getEmailAddress(context, user));
            }
        } else {
            RESULT_SENDER_LOGGER.info("No promote user found. Error mail will be sent to AMS.");
//            recipients.add(ApplicationProperties.getProprtyValue("ams.mail.recipient"));
            recipient.add(PropertyReader.getProperty("genetic.email.default.recipient"));
        }
        return recipient;
    }

    /**
     * Getting Email address based on user name
     *
     * @param context is required for performing MQL Query
     * @param userName contains the user's name whom email address we need
     * @return email address which we have found for the user
     */
    public String getEmailAddress(Context context, String userName) {
        String email = "";
        StringBuilder commandBuilder = new StringBuilder();
        try {
//            Example MQL Command: "print person coexusr1 select email"
            commandBuilder.append("print person ").append(userName)
                    .append(" select email dump");
            String newEmail = MqlUtil.mqlCommand(context, commandBuilder.toString());
//            The email address is the last word of the mql command response
            email = newEmail;
            RESULT_SENDER_LOGGER.info("Email Address : " + email);
        } catch (FrameworkException ex) {
            RESULT_SENDER_LOGGER.error(ex.getMessage());
        }
        return email;
    }

    /**
     * Prepare email body and sends mail to the recipient
     *
     * @param recipient is the email address of recipient
     * @param results is the List of ResponseResult which contains the
     * @param subject takes the email subject information about the item to
     * generate email body
     */
    public void send(String recipient, List<ResponseResult> results, String emailSubject, String tableHeader) {
        StringBuilder emailBody = getEmailBody(results, tableHeader);
        RESULT_SENDER_LOGGER.info("Prepared Email Body: \n" + emailBody.toString() + "\n for " + recipient + " email address");

        if (recipient != "") {
            Map<String, Object> returnResult = sendMail(emailSender, emailBody, recipient, emailSubject);
            RESULT_SENDER_LOGGER.info(returnResult.get("sendResult"));
        } else {
            RESULT_SENDER_LOGGER.error("Email Recipient is Empty! Or No Error Found.");
        }
    }

}
