package com.example.app.registration;

import org.springframework.stereotype.Service;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

@Service
public class EmailVerificationService {

    /**
     * Checks whether the domain part of the email address has valid MX records.
     */
    public boolean hasValidMxRecord(String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }

        String domain = email.substring(email.indexOf('@') + 1).trim();
        if (domain.isBlank()) {
            return false;
        }

        // Configure JNDI to use DNS service provider
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");

        // Critical: Set short timeouts so a bad DNS server doesn't freeze the request
        env.put("com.sun.jndi.dns.timeout.initial", "2000"); // 2 seconds
        env.put("com.sun.jndi.dns.timeout.retries", "1");

        try {
            DirContext ictx = new InitialDirContext(env);
            Attributes attrs = ictx.getAttributes(domain, new String[]{"MX"});
            Attribute attr = attrs.get("MX");

            // No MX records found for this domain
            if (attr == null || attr.size() == 0) {
                System.out.println("DNS CHECK FAILED: No MX records found for domain [" + domain + "]");
                return false;
            }

            // Check for RFC 7505 "Null MX" (domain explicitly stating it does NOT accept mail)
            NamingEnumeration<?> servers = attr.getAll();
            while (servers.hasMore()) {
                String record = servers.next().toString();
                // A Null MX record looks like "0 ."
                if (record.trim().equals("0 .")) {
                    System.out.println("DNS CHECK FAILED: Domain [" + domain + "] publishes Null MX (no mail accepted).");
                    return false;
                }
            }

            System.out.println("DNS CHECK PASSED: Found " + attr.size() + " MX record(s) for [" + domain + "]");
            return true;

        } catch (NamingException e) {
            // Thrown if domain does not exist (NXDOMAIN) or network lookup timed out
            System.out.println("DNS LOOKUP ERROR for [" + domain + "]: " + e.getMessage());
            return false;
        }
    }
}