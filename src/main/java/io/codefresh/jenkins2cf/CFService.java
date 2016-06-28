/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.codefresh.jenkins2cf;

import hudson.util.Secret;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import javax.net.ssl.HttpsURLConnection;

/**
 *
 * @author antweiss
 */
public class CFService {
    private String name;
    private String id;
    private Secret cfToken;

    public CFService(Secret cfToken, String gitRepo, String id) {
        this.name = gitRepo;
        this.cfToken = cfToken;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }
    
}
