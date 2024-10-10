package com.docprocess.manager;



import com.docprocess.config.ConfigConstant;
import com.docprocess.repository.SystemConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Properties;


@Component
public class EmailServiceManager {

    Session mailSession = null;

    @Autowired
    SystemConfigRepository systemConfigRepository;

    public void send(String from,String to,String subject,String content) {
        try {
            createMailSession();
            String emailList[] =  to.split(",");
            MimeMessage message = new MimeMessage(mailSession);

            Multipart multipart = new MimeMultipart("alternative");
            BodyPart part1 = new MimeBodyPart();
            part1.setContent(content,"text/html; charset=UTF-8");
            multipart.addBodyPart(part1);

            message.setFrom(new InternetAddress(from));
            for(String email:emailList){
                message.addRecipient(Message.RecipientType.TO,new InternetAddress(email));
            }
            message.setSubject(subject);
            message.setContent(multipart);

            Transport transport = mailSession.getTransport();
            // Connect the transport object.
            transport.connect();
            // Send the message.
            transport.sendMessage(message, message.getAllRecipients());
            // Close the connection.
            transport.close();
        } catch (MessagingException ex) {
            System.out.print(ex.getMessage());
        } finally {
        }
    }

    private void createMailSession(){
        if( mailSession==null ){
            String SMTP_HOST_NAME = systemConfigRepository.findByConfigKey (ConfigConstant.SENDGRID_SMTP_HOST_NAME).getConfigValue();
            String SMTP_PORT = systemConfigRepository.findByConfigKey(ConfigConstant.SENDGRID_SMTP_PORT).getConfigValue();
            String SMTP_AUTH_USER = systemConfigRepository.findByConfigKey(ConfigConstant.SENDGRID_SMTP_AUTH_USER).getConfigValue();
            String SMTP_AUTH_PWD = systemConfigRepository.findByConfigKey(ConfigConstant.SENDGRID_SMTP_AUTH_PASSWORD).getConfigValue();

            Properties properties = new Properties();
            properties.put("mail.transport.protocol", "smtp");
            properties.put("mail.smtp.host", SMTP_HOST_NAME);
            properties.put("mail.smtp.port", SMTP_PORT);
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable","true");

            Authenticator auth = new SMTPAuthenticator(SMTP_AUTH_USER,SMTP_AUTH_PWD);
            mailSession = Session.getDefaultInstance(properties,auth);
        }
    }

    private class SMTPAuthenticator extends javax.mail.Authenticator {
        String username;
        String password;
        public SMTPAuthenticator(String username,String password){
            this.username=username;
            this.password=password;
        }
        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password);
        }
    }
}
