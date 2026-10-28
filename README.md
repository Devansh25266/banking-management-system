# Banking Management System

A full-stack Banking Management System developed using **Java, Spring Boot, MySQL, React, TypeScript, and Tailwind CSS**. This project simulates basic banking operations through a responsive web application with customer functionality and an administrator console.

## Overview

The Banking Management System is an educational project designed to demonstrate full-stack web development, database management, REST API integration, authentication, and secure handling of banking-related operations. It connects a React-based frontend with a Java Spring Boot backend and a MySQL database.

## Features

- **User Authentication:** Customer registration, login, and logout.
- **Customer Management:** Administrator access to customer information.
- **Bank Account Management:** Savings and Current accounts with generated account numbers.
- **Deposit and Withdrawal:** Record transactions and update account balances.
- **Transaction History:** View transaction records with filtering and sorting.
- **Admin Dashboard:** Overview of banking data and administrative activities.
- **Account Controls:** Freeze, unfreeze, and close accounts.
- **Audit Logs:** Track administrative actions.
- **Role-Based Access:** Separate customer and administrator functionality.
- **Responsive Interface:** Clean web interface built with React and Tailwind CSS.

## Technology Stack

| Component | Technology |
|---|---|
| Programming Language | Java |
| Backend Framework | Spring Boot |
| Database | MySQL 8 |
| Database Connectivity | JDBC Template |
| Frontend | React and TypeScript |
| Styling | Tailwind CSS |
| Build Tool | Maven |
| API Architecture | REST APIs |

## Project Structure

```text
banking-management-system/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   └── test/
│   └── pom.xml
├── frontend/
│   ├── src/
│   ├── package.json
│   └── package-lock.json
├── .gitignore
└── README.md
```

## Getting Started

### Prerequisites

Install the following software:

- Java Development Kit (JDK 17 or compatible version)
- Maven
- MySQL Server
- Node.js and npm

### Backend Setup

1. Create a MySQL database named `banking_db`.
2. Configure the database connection using environment variables.
3. Execute the provided SQL schema to create the required tables.
4. Open a terminal in the `backend` directory and run:

```bash
mvn spring-boot:run
```

The backend runs on `http://localhost:8080` by default.

### Frontend Setup

Open another terminal in the `frontend` directory and run:

```bash
npm install
npm run dev
```

Open the local URL displayed in your terminal, usually `http://localhost:5173`.

Configure the required administrator and database environment variables according to your local setup.

## Learning Objectives

This project provides practical experience in Java backend development, React frontend development, MySQL database integration, REST API design, authentication, and full-stack application architecture.

## Disclaimer

This project is an **educational banking simulation**, not a real banking application. It is not intended for actual financial transactions or storing real customer financial information.

## Author

Developed as a full-stack software development project using modern web technologies.
