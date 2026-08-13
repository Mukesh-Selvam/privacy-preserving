package com.hackathon.gateway.dto;

public class OpaInput {

    private Inner input;

    public OpaInput(String orgId, String field, boolean consentGiven) {
        this.input = new Inner(orgId, field, consentGiven);
    }

    public Inner getInput() { return input; }
    public void setInput(Inner input) { this.input = input; }

    public static class Inner {
        private String org_id;
        private String field;
        private boolean consent_given;

        public Inner(String orgId, String field, boolean consentGiven) {
            this.org_id = orgId;
            this.field = field;
            this.consent_given = consentGiven;
        }

        public String getOrg_id() { return org_id; }
        public void setOrg_id(String org_id) { this.org_id = org_id; }

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }

        public boolean isConsent_given() { return consent_given; }
        public void setConsent_given(boolean consent_given) { this.consent_given = consent_given; }
    }
}
