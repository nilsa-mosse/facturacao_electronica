package ao.co.hzconsultoria.efacturacao.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Controller
public class AcessoNegadoController {

    @GetMapping("/acesso-negado")
    public String acessoNegado(@RequestParam(value = "endpoint", required = false) String endpoint,
                               @RequestParam(value = "url", required = false) String url,
                               @RequestParam(value = "mensagem", required = false) String mensagem,
                               HttpServletRequest request,
                               Model model) {
        
        String targetUri = endpoint != null && !endpoint.isEmpty() ? endpoint : url;
        if (targetUri == null || targetUri.isEmpty()) {
            targetUri = "/superadmin/licenca/gerador";
        } else {
            try {
                targetUri = URLDecoder.decode(targetUri, StandardCharsets.UTF_8.name());
            } catch (Exception ignored) {}
        }

        String fullUrl = targetUri;
        if (!targetUri.startsWith("http://") && !targetUri.startsWith("https://")) {
            String scheme = request.getScheme();
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            fullUrl = scheme + "://" + serverName + (serverPort == 80 || serverPort == 443 ? "" : ":" + serverPort) + targetUri;
        }

        if (mensagem == null || mensagem.isEmpty()) {
            mensagem = "Proibição de Acesso à Página: O seu perfil de utilizador não possui permissão para aceder ao endereço solicitado (" + fullUrl + ").";
        } else {
            try {
                mensagem = URLDecoder.decode(mensagem, StandardCharsets.UTF_8.name());
            } catch (Exception ignored) {}
        }

        model.addAttribute("endpoint", targetUri);
        model.addAttribute("fullUrl", fullUrl);
        model.addAttribute("mensagemProibicao", mensagem);

        return "acesso-negado";
    }
}
