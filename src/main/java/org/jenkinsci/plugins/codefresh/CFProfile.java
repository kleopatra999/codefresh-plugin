/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
 package org.jenkinsci.plugins.codefresh;

import hudson.util.Secret;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author antweiss
 */
public class CFProfile {
    private final String cfUser;
    private final Secret cfToken;
    private List<CFService> services;
    private CFApi api;
    //private List<CFService> services;

    public CFProfile(String cfUser, Secret cfToken) throws IOException {
        this.cfUser = cfUser;
        this.cfToken = cfToken;
        this.api = new CFApi(this.cfToken);
        this.services = api.getServices();
    }

    public String getUser(){
        return this.cfUser;
    }

    public Secret getToken(){
        return this.cfToken;
    }

    public int testConnection() throws IOException{
       api = new CFApi(cfToken);
       
       try{
           api.getConnection("");
       }
       catch (Exception e)
       {
            throw e;
       }
       return 0;
    }

    String getServiceIdByName(String serviceName) {
        for (CFService service: services){
            if (service.getName().equals(serviceName))
            {
                return service.getId();
            }
        }
        return null;
    }

    String getServiceIdByPath(String gitPath) {
        String serviceName = gitPath.split("/")[2].split("\\.")[0];      
        for (CFService service: services){
            if (service.getName().equals(serviceName))
            {
                    return service.getId();
            }
        }
        return null;
    }

}
