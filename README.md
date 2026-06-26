# 📦 IT Inventory Management System

![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/springboot-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![MySQL](https://img.shields.io/badge/mysql-%2300000f.svg?style=for-the-badge&logo=mysql&logoColor=white)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)

A full-stack, containerized web application designed to streamline the tracking and management of hardware equipment. This project serves as a dedicated support tool, providing a RESTful API for inventory operations and a lightweight frontend interface.

## 🏗️ System Architecture & Isolation

This application is built with security and network isolation in mind. It is designed to run securely behind an API Gateway (Reverse Proxy).

- **Backend & Database Isolation:** The Spring Boot backend and MySQL database communicate through a private, internal Docker network (`inventario-net`). The database is entirely inaccessible from the outside world.
- **API Gateway Integration:** The frontend and backend are partially exposed to a shared external network (`global-router`), allowing the central Reverse Proxy to route traffic to them securely without mapping ports directly to the host machine.

## 💻 Tech Stack

- **Frontend:** HTML, JavaScript (Fetch API), served via an internal Nginx container.
- **Backend:** Java 17, Spring Boot, Spring Data JPA.
- **Database:** MySQL 8.0 (with automated initialization via `init.sql`).
- **Infrastructure:** Docker & Docker Compose.
