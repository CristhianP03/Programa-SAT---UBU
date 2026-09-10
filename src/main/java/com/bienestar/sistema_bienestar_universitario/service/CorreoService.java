package com.bienestar.sistema_bienestar_universitario.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CorreoService {

    private final JavaMailSender mailSender;

    @Value("${app.nombre}")
    private String appNombre;

    @Value("${spring.mail.username}")
    private String correoOrigen;

    // Correo informativo al estudiante al registrarse.
    // Ya no requiere verificación: entra directo a la cola.
    public void enviarCorreoRegistroInformativo(String correoDestino,
                                                  String nombreEstudiante,
                                                  String area,
                                                  String servicio,
                                                  String turnoTexto) {
        String asunto = appNombre + " — Registro confirmado";
        String cuerpo = "Hola " + nombreEstudiante + ",\n\n"
                + "Tu registro fue creado correctamente. "
                + "Ya estás en la cola de atención de tu área.\n\n"
                + "  Área    : " + area + "\n"
                + "  Servicio: " + servicio + "\n"
                + "  Turno   : " + turnoTexto + "\n\n"
                + "Aguarda tu turno.\n\n"
                + appNombre;

        enviar(correoDestino, asunto, cuerpo);
    }

    @Async("mailExecutor")
    public void enviarCorreoRegistroInformativoAsync(String correoDestino,
                                                       String nombreEstudiante,
                                                       String area,
                                                       String servicio,
                                                       String turnoTexto) {
        try {
            enviarCorreoRegistroInformativo(correoDestino, nombreEstudiante, area, servicio, turnoTexto);
        } catch (Exception e) {
            log.error("Error async correo registro a {}: {}", correoDestino, e.getMessage());
        }
    }

    public void enviarNotificacionAcceso(String correoDestino,
                                           String nombreUsuario,
                                           String ip,
                                           String fechaHora) {
        String asunto = appNombre + " — Nuevo acceso registrado";
        String cuerpo = "Hola " + nombreUsuario + ",\n\n"
                + "Se registró un inicio de sesión en tu cuenta:\n"
                + "  Fecha y hora : " + fechaHora + "\n"
                + "  IP de acceso : " + ip + "\n\n"
                + "Si fuiste tú, puedes ignorar este mensaje.\n"
                + "Si NO fuiste tú, contacta al administrador del sistema.\n\n"
                + appNombre;

        enviar(correoDestino, asunto, cuerpo);
    }

    @Async("mailExecutor")
    public void enviarNotificacionAccesoAsync(String correoDestino,
                                                String nombreUsuario,
                                                String ip,
                                                String fechaHora) {
        try {
            enviarNotificacionAcceso(correoDestino, nombreUsuario, ip, fechaHora);
        } catch (Exception e) {
            log.error("Error async notificacion acceso a {}: {}", correoDestino, e.getMessage());
        }
    }

    public void enviarNotificacionNuevoTurno(String correoEncargado,
                                               String nombreEncargado,
                                               String nombreEstudiante,
                                               String servicio,
                                               String turnoTexto) {
        String asunto = appNombre + " — Nuevo estudiante en cola";
        String cuerpo = "Hola " + nombreEncargado + ",\n\n"
                + "El estudiante " + nombreEstudiante
                + " se registró y ya está en la cola de tu área.\n\n"
                + "  Servicio : " + servicio + "\n"
                + "  Turno    : " + turnoTexto + "\n\n"
                + "Atiende su solicitud cuando esté disponible.\n\n"
                + appNombre;

        enviar(correoEncargado, asunto, cuerpo);
    }

    @Async("mailExecutor")
    public void enviarNotificacionNuevoTurnoAsync(String correoEncargado,
                                                    String nombreEncargado,
                                                    String nombreEstudiante,
                                                    String servicio,
                                                    String turnoTexto) {
        try {
            enviarNotificacionNuevoTurno(correoEncargado, nombreEncargado, nombreEstudiante, servicio, turnoTexto);
        } catch (Exception e) {
            log.error("Error async nuevo turno a {}: {}", correoEncargado, e.getMessage());
        }
    }

    // Contraseña temporal que el administrador envía al crear
    // un usuario sin contraseña inicial.
    public void enviarContrasenaTemporal(String correoDestino,
                                           String nombreUsuario,
                                           String usuario,
                                           String contrasenaTemporal) {
        String asunto = appNombre + " — Credenciales de acceso";
        String cuerpo = "Hola " + nombreUsuario + ",\n\n"
                + "Te fue creada una cuenta en el sistema. "
                + "Usa estas credenciales para iniciar sesión:\n\n"
                + "  Usuario    : " + usuario + "\n"
                + "  Contraseña : " + contrasenaTemporal + "\n\n"
                + "Te recomendamos cambiarla desde tu perfil una vez que ingreses.\n\n"
                + appNombre;

        enviar(correoDestino, asunto, cuerpo);
    }

    @Async("mailExecutor")
    public void enviarContrasenaTemporalAsync(String correoDestino,
                                                String nombreUsuario,
                                                String usuario,
                                                String contrasenaTemporal) {
        try {
            enviarContrasenaTemporal(correoDestino, nombreUsuario, usuario, contrasenaTemporal);
        } catch (Exception e) {
            log.error("Error async contraseña temporal a {}: {}", correoDestino, e.getMessage());
        }
    }

    private void enviar(String destino, String asunto, String cuerpo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(correoOrigen);
        mensaje.setTo(destino);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);
        mailSender.send(mensaje);
    }
}
