package ao.co.hzconsultoria.efacturacao.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import ao.co.hzconsultoria.efacturacao.service.SaftService;
import java.util.Date;
import java.text.SimpleDateFormat;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/saft")
public class SaftController {

    private final SaftService saftService;

    public SaftController(SaftService saftService) {
        this.saftService = saftService;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportSaft(
            @RequestParam("inicio") String inicio,
            @RequestParam("fim") String fim) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date startDate = sdf.parse(inicio);
            Date endDate = sdf.parse(fim);

            String xml = saftService.generateSaftXml(startDate, endDate);
            byte[] bytes = xml.getBytes("UTF-8");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.setContentDisposition(ContentDisposition.attachment().filename("SAFT_AO_" + inicio + "_" + fim + ".xml").build());

            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/enviar")
    public ResponseEntity<String> enviarParaAgt(@RequestParam("inicio") String inicio, @RequestParam("fim") String fim) {
        // Lógica de simulação de envio para AGT
        return ResponseEntity.ok("Ficheiro SAF-T enviado com sucesso para a AGT (Ambiente de Homologação). Protocolo: AGT-" + System.currentTimeMillis());
    }
}
