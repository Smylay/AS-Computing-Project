package com.smylay.hr;

import com.smylay.hr.model.Absence;
import com.smylay.hr.model.Employee;
import java.util.List;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.mail.PasswordAuthentication;
import javax.mail.internet.InternetAddress;
import org.jboss.logging.Logger;

// This bean is used to send users emails.
public class MailSenderBean {

    private static final Logger log = Logger.getLogger(MailSenderBean.class);

    /**
     * Sends an email to the managers to inform them of a new absence request.
     *
     * @param managers = list of managers
     * @param employee = the employee requesting the absence
     *
     * @return none
     */
    public static void sendNewRequestEmail(List<Employee> managers, Employee employee) {
        // Setup the mail server and create a new message.
        Session session = setupMailServer();
        Message msg = new MimeMessage(session);

        try {
            // Setup from address.
            msg.setFrom(new InternetAddress(Constants.FROM_EMAIL_ADDRESS));

            // iterate over all managers to add them as recipients.
            for (Employee thisEmployee : managers) {

                //Create an internet address for each manager.
                Address addr = new InternetAddress(thisEmployee.getEmail());

                // Add to the recipient list.
                msg.addRecipient(Message.RecipientType.TO, addr);
            }

            // Set the subject
            msg.setSubject("New Absence Request");

            // Set the content of the email.
            msg.setContent("A new absence has been requested by " + employee.getName(), "text/html");

            // Send the message
            Transport.send(msg);

        } catch (Exception e) {

            log.error("Failed to send", e);

        }

    }

    /**
     * Sends an email an employee to inform them of the approval status of their absence request.
     *
     * @param managers = list of managers
     * @param employee = the employee requesting the absence
     *
     * @return none
     */
    public static void sendApprovedRequestMailer(Employee employee, Absence absence) {
        // Setup the mail server and create a new message.
        Session session = setupMailServer();
        Message msg = new MimeMessage(session);

        try {
            // Setup from address.
            msg.setFrom(new InternetAddress(Constants.FROM_EMAIL_ADDRESS));

            // Create an internet address for the employee and add as a recipient.
            Address addr = new InternetAddress(employee.getEmail());
            msg.addRecipient(Message.RecipientType.TO, addr);

            // Set the subject.
            msg.setSubject("Absence Request Update");

            // Set the content
            String defaultMsg = " your absence request has ";
            String approved;
            if (absence.getApproved()) {
                approved = "been approved";
            } else {
                approved = "been denied";
            }
            msg.setContent(employee.getName() + defaultMsg + approved, "text/html");

            // Send the message.
            Transport.send(msg);

        } catch (Exception e) {

            log.error("Failed to send", e);

        }
    }

    /**
     * A utility method to set up the mail server.
     *
     * @param none
     *
     * @return the mail session.
     */
    private static Session setupMailServer() {
        Properties mailProps = new Properties();
        mailProps.put("mail.smtp.host", Constants.MAIL_SERVER);
        mailProps.put("mail.smtp.auth", "true");
        mailProps.put("mail.smtp.port", Constants.MAIL_SERVER_PORT);

        // Use authentication.
        Session session = Session.getInstance(mailProps, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(Constants.MAIL_SERVER_USERNAME, Constants.MAIL_SERVER_PASSWORD);
            }
        });
        return session;
    }

}
