package com.hackathon.gateway.aspect;

import com.hackathon.gateway.annotation.PrivacyProtected;
import com.hackathon.gateway.service.GatewayService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * AspectJ AOP advisor that intercepts methods annotated with {@link PrivacyProtected}.
 *
 * <p>Extracts patient ID and requesting organization parameters from the join point,
 * delegates field-level privacy evaluation to {@link GatewayService}, and transforms
 * the returned data record transparently.
 */
@Aspect
@Component
public class PrivacyGatewayAspect {

    private static final Logger log = LoggerFactory.getLogger(PrivacyGatewayAspect.class);

    private final GatewayService gatewayService;

    public PrivacyGatewayAspect(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @Around("@annotation(privacyProtected)")
    public Object applyPrivacyFiltering(ProceedingJoinPoint joinPoint, PrivacyProtected privacyProtected) throws Throwable {
        Object result = joinPoint.proceed();
        if (result == null) {
            return null;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        Integer patientId = extractIntegerParam(paramNames, args, privacyProtected.patientIdParam());
        String orgId = extractStringParam(paramNames, args, privacyProtected.orgParam());

        if (patientId == null || orgId == null) {
            log.warn("[PrivacyAspect] Could not resolve patientId or orgId from method parameters for {}, skipping aspect filtering", method.getName());
            return result;
        }

        log.info("[PrivacyAspect] Intercepted method {}: applying dual-gate policy for patientId={}, orgId={}", method.getName(), patientId, orgId);

        // Fetch transformed record through the core privacy gateway
        Map<String, Object> filteredRecord = gatewayService.fetchProtectedRecord(patientId, orgId, "aspect-client", "127.0.0.1");

        return filteredRecord;
    }

    private Integer extractIntegerParam(String[] names, Object[] args, String targetName) {
        if (names == null || args == null) return null;
        for (int i = 0; i < names.length; i++) {
            if (names[i].equalsIgnoreCase(targetName) && args[i] != null) {
                if (args[i] instanceof Integer num) return num;
                try {
                    return Integer.parseInt(args[i].toString());
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    private String extractStringParam(String[] names, Object[] args, String targetName) {
        if (names == null || args == null) return null;
        for (int i = 0; i < names.length; i++) {
            if (names[i].equalsIgnoreCase(targetName) && args[i] != null) {
                return args[i].toString();
            }
        }
        return null;
    }
}
