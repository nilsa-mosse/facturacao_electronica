package ao.co.hzconsultoria.efacturacao.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public String handleAccessDeniedException(org.springframework.security.access.AccessDeniedException e, HttpServletRequest request) {
        try {
            String uri = request.getRequestURI();
            return "redirect:/acesso-negado?endpoint=" + java.net.URLEncoder.encode(uri, "UTF-8");
        } catch (Exception ex) {
            return "redirect:/acesso-negado?endpoint=/superadmin/licenca/gerador";
        }
    }

    @ExceptionHandler(Exception.class)
    @org.springframework.web.bind.annotation.ResponseBody
    public String handleException(Exception e, HttpServletRequest request) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String logMsg = "Exception at " + request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : "") + "\n" + sw.toString() + "\n===============\n";
            Files.write(Paths.get("d:/eclipse_workspace/facturacao_electronica/error_log.txt"), logMsg.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception writingEx) {
            // ignore
        }
        return "Ocorreu um erro interno no servidor. Por favor, verifique o ficheiro error_log.txt ou contacte o administrador. Detalhe: " + e.getMessage();
    }
}
