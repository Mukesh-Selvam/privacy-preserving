package gateway

# Returns one of: "plain", "encrypted", "hidden" for a given
# {org_id, field, consent_given} input.
#
# The innovation: every rule below requires consent_given == true.
# Even when an organization's policy allows a field, the patient's
# consent is a second, independent gate - both must pass.

default field_mode := "hidden"

# ---- insurer-partner : claims-processing access ----

field_mode := "plain" if {
    input.org_id == "insurer-partner"
    input.field == "name"
    input.consent_given == true
}

field_mode := "plain" if {
    input.org_id == "insurer-partner"
    input.field == "age"
    input.consent_given == true
}

field_mode := "plain" if {
    input.org_id == "insurer-partner"
    input.field == "disease"
    input.consent_given == true
}

field_mode := "encrypted" if {
    input.org_id == "insurer-partner"
    input.field == "aadhaar"
    input.consent_given == true
}

# phone and address are never allowed for insurer-partner,
# so no rule exists for them - they fall through to the "hidden" default.

# ---- research-org : de-identified research access ----

field_mode := "plain" if {
    input.org_id == "research-org"
    input.field == "age"
    input.consent_given == true
}

field_mode := "plain" if {
    input.org_id == "research-org"
    input.field == "disease"
    input.consent_given == true
}
