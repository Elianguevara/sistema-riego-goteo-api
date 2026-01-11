# 🌱 Sistema de Riego por Goteo - Backend API

![Java](https://img.shields.io/badge/Java-17-ed8b00?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.10-6db33f?style=for-the-badge&logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479a1?style=for-the-badge&logo=mysql)
![JWT](https://img.shields.io/badge/Security-JWT-black?style=for-the-badge&logo=jsonwebtokens)

## 📖 Descripción

Esta API RESTful es el núcleo backend para una plataforma integral de **gestión y automatización de riego agrícola**. El sistema permite a los administradores y agricultores gestionar fincas, monitorear sensores de humedad en tiempo real, programar turnos de riego y generar reportes analíticos para optimizar el consumo de agua y energía.

El proyecto está construido con una arquitectura robusta utilizando **Spring Boot**, implementando seguridad con **Spring Security & JWT**, e integraciones con servicios externos de clima y geolocalización.

## 🚀 Características Principales

### 💧 Gestión Agrícola
* **Administración de Fincas y Sectores:** ABM completo de zonas de cultivo.
* **Control de Riego:** Programación y registro de turnos de riego y fertilización.
* **Fuentes de Agua:** Gestión de pozos, embalses y turnos de reserva.

### 📡 Monitoreo y Sensores
* **Sensores IoT:** Registro de lecturas de humedad del suelo.
* **Alertas:** Sistema de notificaciones automáticas basadas en umbrales de humedad.
* **Integración Climática:** Conexión con **OpenWeatherMap** para obtener datos meteorológicos en tiempo real y optimizar el riego.

### 📊 Reportes y Analítica
* **Dashboards:** KPIs para administradores y analistas (consumo energético, balance hídrico).
* **Exportación de Datos:** Generación de reportes en **PDF, Excel y CSV** (usando OpenPDF y Apache POI).
* **Auditoría:** Registro detallado de operaciones y cambios en el sistema.

### 📱 Sincronización
* **Soporte Offline-First:** Endpoints dedicados para la sincronización de datos con aplicaciones móviles (`MobileSyncController`).

## 🛠️ Stack Tecnológico

* **Lenguaje:** Java 17
* **Framework:** Spring Boot 3.4.10
* **Base de Datos:** MySQL
* **Seguridad:** Spring Security, JWT (JSON Web Tokens)
* **Persistencia:** Spring Data JPA / Hibernate
* **Documentación:** OpenAPI / Swagger UI
* **Utilidades:** Lombok, ModelMapper
* **APIs Externas:** OpenWeatherMap, OpenCage Geocoding

## ⚙️ Instalación y Configuración

### Prerrequisitos
* Java JDK 17+
* Maven 3.8+
* MySQL Server

### 1. Clonar el repositorio
```bash
git clone [https://github.com/elianguevara/sistema-riego-goteo-api.git](https://github.com/elianguevara/sistema-riego-goteo-api.git)
cd sistema-riego-goteo-api
