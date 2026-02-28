# 🌱 Sistema de Riego por Goteo - Backend API

<div align="center">
  <img src="https://img.shields.io/badge/Java-17-ed8b00?style=for-the-badge&logo=openjdk" alt="Java 17" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.4.10-6db33f?style=for-the-badge&logo=springboot" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/MySQL-8.0-4479a1?style=for-the-badge&logo=mysql" alt="MySQL" />
  <img src="https://img.shields.io/badge/Security-JWT-black?style=for-the-badge&logo=jsonwebtokens" alt="JWT Security" />
  <img src="https://img.shields.io/badge/Hibernate-ORM-59666C?style=for-the-badge&logo=Hibernate" alt="Hibernate" />
</div>

<br>

## 📖 Descripción

Esta API RESTful es el núcleo backend para una plataforma integral de **gestión y automatización de riego agrícola**. Diseñada meticulosamente para escalar, la plataforma permite a los administradores y agricultores (operarios) gestionar fincas, monitorear sensores de humedad en tiempo real, programar turnos de riego y generar reportes analíticos para optimizar el consumo de agua y energía.

El proyecto está construido con una arquitectura robusta orientada a servicios utilizando **Spring Boot**, implementando seguridad avanzada basada en roles con **Spring Security & JWT**, un completo rastro de auditoría, e integraciones con servicios externos de clima e IoT.

---

## 🚀 Características Principales

### 💧 Gestión Agrícola y de Riego
* **Administración de Fincas y Sectores:** ABM completo de zonas de cultivo, permitiendo asignar sectores a fincas y operarios a fincas específicas.
* **Control de Riego y Fertilizantes:** Programación detallada, registro de turnos de riego, control de consumo de agua (m3) y gestión de fertilización.
* **Fuentes de Agua:** Gestión de distintos tipos de fuentes (pozos, embalses, red) y sus capacidades, junto a los turnos de reserva.

### 📡 Monitoreo, Sensores y Clima
* **Sensores IoT:** Registro y monitoreo de lecturas de humedad del suelo con histórico de datos.
* **Alertas Inteligentes:** Sistema de notificaciones automáticas y eventos (SSE/WebSockets) basadas en umbrales de humedad críticos o condiciones anómalas.
* **Integración Meteorológica:** Conexión con **OpenWeatherMap** para obtener datos climáticos actuales y pronósticos, además de registro de precipitaciones.

### 📊 Reportes, Analítica y Auditoría
* **Dashboards y Estadísticas:** KPIs para administradores (consumo energético, balance hídrico mensual, tiempo de riego).
* **Exportación de Datos:** Generación dinámica de reportes en formatos **PDF, Excel y CSV**.
* **Auditoría Completa:** Registro detallado y automático de creación, actualización y eliminación de entidades críticas (rastreabilidad total del sistema).

### 📱 Movilidad y Sincronización
* **Offline-First Ready:** Endpoints dedicados (`MobileSyncController`) para la sincronización bidireccional de datos con aplicaciones móviles que operan sin conexión en el campo.

---

## 🛠️ Stack Tecnológico

* **Lenguaje Core:** Java 17
* **Framework Principal:** Spring Boot 3.4.10
* **Base de Datos:** MySQL 8+
* **Seguridad y Autenticación:** Spring Security, JWT (JSON Web Tokens)
* **Persistencia y ORM:** Spring Data JPA / Hibernate
* **Documentación de API:** OpenAPI 3.0 (Swagger UI)
* **Herramientas y Librerías:** 
  * Lombok (Reducción de boilerplate)
  * ModelMapper (Mapeo DTO-Entity)
  * OpenPDF / Apache POI (Generación de documentos)
* **APIs Externas Integradas:** OpenWeatherMap, OpenCage Geocoding

---

## ⚙️ Instalación y Configuración Local

### Prerrequisitos
* **Java JDK 17** o superior instalado en tu variable de entorno PATH.
* **Maven 3.8+** (Aunque el proyecto incluye el wrapper `mvnw`).
* **MySQL Server** corriendo localmente o un contenedor Docker.

### 1. Clonar el repositorio
```bash
git clone https://github.com/elianguevara/sistema-riego-goteo-api.git
cd sistema-riego-goteo-api
```

### 2. Configurar la Base de Datos
Crea una base de datos en MySQL para el proyecto:
```sql
CREATE DATABASE sistema_riego;
```

### 3. Variables de Entorno y `application.properties`
Renombra el archivo `application-dev.properties.example` (si existe) o asegúrate de que tu `src/main/resources/application.properties` tenga la configuración correcta de acceso a datos y las claves secretas:

```properties
# Base de Datos
spring.datasource.url=jdbc:mysql://localhost:3306/sistema_riego?useSSL=false&serverTimezone=UTC
spring.datasource.username=TU_USUARIO_MYSQL
spring.datasource.password=TU_PASSWORD_MYSQL

# Hibernate Config (Actualiza el esquema automáticamente en desarrollo)
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# JWT Secret Key (Debe ser una cadena base64 larga y segura)
jwt.secret=PON_AQUI_UNA_CLAVE_SECRETA_LARGA_EN_BASE64
jwt.expiration=86400000

# OpenWeatherMap API Key
openweathermap.api.key=tu_api_key_aqui
```

### 4. Compilar y Ejecutar
Puedes ejecutar el proyecto directamente usando el wrapper de Maven:

**En Windows:**
```cmd
.\mvnw clean install
.\mvnw spring-boot:run
```

**En Linux / macOS:**
```bash
./mvnw clean install
./mvnw spring-boot:run
```

El servidor iniciará por defecto en `http://localhost:8080`.

---

## 📚 Documentación de la API (Swagger UI)

Una vez que la aplicación esté corriendo, puedes explorar e interactuar con todos los endpoints (REST) gráficamente de la API a través de Swagger UI.

👉 **Accede a Swagger en:** `http://localhost:8080/swagger-ui/index.html`

Para probar los endpoints protegidos, primero debes hacer login en el endpoint `/api/auth/login`, copiar el token de respuesta, y hacer clic en el botón **"Authorize"** en Swagger, pegando el token en el formato: `Bearer <tu_token>`.

---

## 🤝 Contribución

¡Cualquier mejora es bienvenida! Si deseas contribuir:
1. Haz un Fork del repositorio.
2. Crea una Feature Branch (`git checkout -b feature/NuevaFuncionalidad`).
3. Haz Commit de tus cambios (`git commit -m 'Añade NuevaFuncionalidad'`).
4. Haz Push a la rama (`git push origin feature/NuevaFuncionalidad`).
5. Abre un Pull Request.

---

## 🛡️ Licencia

Este proyecto fue desarrollado bajo el contexto de una Práctica Profesionalizante. Todos los derechos reservados.
