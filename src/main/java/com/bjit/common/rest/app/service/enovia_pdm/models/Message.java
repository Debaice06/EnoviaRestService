/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bjit.common.rest.app.service.enovia_pdm.models;

import com.bjit.common.rest.app.service.model.tnr.TNR;

/**
 *
 * @author BJIT
 */
public class Message {

    private TNR tnr;
    private String errorMessage;

    public Message() {
    }

    public Message(TNR tnr, String errorMessage) {
        this.tnr = tnr;
        this.errorMessage = errorMessage;
    }

    public TNR getTnr() {
        return tnr;
    }

    public void setTnr(TNR tnr) {
        this.tnr = tnr;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

}
