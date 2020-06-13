package org.github.leleueri;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Singleton;

@Singleton
public class PictorgaConfiguration {
    @ConfigProperty(name = "pictorga.repository")
    private String repository;
    
    @ConfigProperty(name = "pictorga.input")
    private String inputDir;

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getInputDir() {
        return inputDir;
    }

    public void setInputDir(String inputDir) {
        this.inputDir = inputDir;
    }
}
