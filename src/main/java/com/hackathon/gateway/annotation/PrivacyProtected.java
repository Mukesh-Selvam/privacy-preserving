package com.hackathon.gateway.annotation;

import java.lang.annotation.*;

/**
 * Indicates that a method returns sensitive patient or user data subject to
 * dual-gated privacy filtering (OPA policy $\cap$ Patient consent).
 *
 * <p>When annotated on a controller or service method, the {@link com.hackathon.gateway.aspect.PrivacyGatewayAspect}
 * automatically intercepts the return value and applies field-level masking,
 * FPE encryption, or redaction.
 *
 * <p>Example:
 * <pre>
 * &#64;GetMapping("/patient/{patientId}")
 * &#64;PrivacyProtected(patientIdParam = "patientId", orgParam = "orgId")
 * public PatientRecord getRecord(&#64;PathVariable Integer patientId, &#64;RequestParam String orgId) {
 *     return patientService.findById(patientId);
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PrivacyProtected {

    /** Name of the method parameter containing the numeric patient/user ID. Defaults to "patientId". */
    String patientIdParam() default "patientId";

    /** Name of the method parameter containing the requesting organisation ID. Defaults to "orgId". */
    String orgParam() default "orgId";
}
