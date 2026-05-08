package ao.co.hzconsultoria.efacturacao.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static Long getCurrentEmpresaId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) auth.getPrincipal()).getEmpresaId();
        }
        return null;
    }

    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) auth.getPrincipal()).getId();
        }
        return null;
    }

    public static boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            String role = ((CustomUserDetails) auth.getPrincipal()).getRole();
            return "SUPERADMIN".equals(role) || "ROLE_SUPERADMIN".equals(role);
        }
        return false;
    }

    public static boolean isAnyAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            String role = ((CustomUserDetails) auth.getPrincipal()).getRole();
            return role != null && (role.contains("ADMIN") || role.contains("SUPERADMIN"));
        }
        return false;
    }

    /**
     * Obtém a empresa do utilizador autenticado
     * Útil para garantir que cada utilizador acessa apenas a sua empresa
     */
    public static CustomUserDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) auth.getPrincipal();
        }
        return null;
    }

    /**
     * Verifica se o utilizador autenticado tem acesso à empresa especificada
     * Útil para validações de segurança
     */
    public static boolean temAcessoEmpresa(Long empresaId) {
        Long empresaIdAtual = getCurrentEmpresaId();
        return empresaIdAtual != null && empresaIdAtual.equals(empresaId);
    }
}

