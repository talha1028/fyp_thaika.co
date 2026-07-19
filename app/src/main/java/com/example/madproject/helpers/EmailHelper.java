package com.example.madproject.helpers;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Writes docs to the "mail" Firestore collection consumed by the Firebase
 * "Trigger Email from Firestore" extension, which sends via SMTP.
 */
public class EmailHelper {
    private static final String MAIL_COLLECTION = "mail";

    public static Task<Void> sendContractEmail(String toEmail, String jobTitle, String pdfUrl) {
        Map<String, Object> message = new HashMap<>();
        message.put("subject", "Your RebuildPak Contract: " + jobTitle);
        message.put("html",
                "<p>Hi,</p>"
                        + "<p>Your construction contract for <b>" + jobTitle + "</b> has been generated.</p>"
                        + "<p><a href=\"" + pdfUrl + "\">Download Contract PDF</a></p>"
                        + "<p>Note: this is an AI-generated template. Please review carefully and consult a "
                        + "legal professional before treating it as binding.</p>"
                        + "<p>— RebuildPak</p>");

        Map<String, Object> mailDoc = new HashMap<>();
        mailDoc.put("to", Arrays.asList(toEmail));
        mailDoc.put("message", message);

        return FirebaseFirestore.getInstance()
                .collection(MAIL_COLLECTION)
                .add(mailDoc)
                .continueWith(task -> null);
    }
}
