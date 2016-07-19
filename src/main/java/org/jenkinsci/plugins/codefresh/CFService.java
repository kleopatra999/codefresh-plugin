/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.codefresh;

import hudson.util.Secret;

/**
 *
 * @author antweiss
 */
public class CFService {
    private final String name;
    private final String id;
    private final Secret cfToken;

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
