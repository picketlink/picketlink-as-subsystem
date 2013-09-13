package test.org.picketlink.as.subsystem.module.idm;

import org.picketlink.idm.model.annotation.AttributeProperty;
import org.picketlink.idm.model.basic.Agent;

import java.io.Serializable;

/**
 * @author Pedro Igor
 */
public class SaleAgent extends Agent implements Serializable {

    @AttributeProperty
    private String status;

    private String password;

    @AttributeProperty
    private String location;

    public SaleAgent() {

    }

    public SaleAgent(String loginName) {
        super(loginName);
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getLocation() {
        return this.location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
