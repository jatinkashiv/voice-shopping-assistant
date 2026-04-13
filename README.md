# voice-shopping-assistant

A professional monorepo for a voice command shopping assistant. The project combines a Spring Boot backend with a React + Vite frontend to turn spoken shopping commands into application actions.

## Features

- Voice-driven shopping assistant workflow
- Voice-to-action request handling between frontend and backend
- Spring Boot REST API for shopping operations
- React interface powered by Vite for fast local development

## Tech Stack

- Backend: Java, Spring Boot, Maven Wrapper
- Frontend: React, Vite, npm

## Repository Structure

```text
voice-shopping-assistant/
├── backend/
│   ├── src/
│   ├── pom.xml
│   ├── mvnw
│   └── mvnw.cmd
├── frontend/
│   ├── public/
│   ├── src/
│   ├── package.json
│   └── vite.config.js
├── .gitignore
└── README.md
```

## How To Run

### Backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The backend runs independently as a standard Spring Boot application.

### Frontend

```powershell
cd frontend
npm install
npm run dev
```

The frontend runs independently with the Vite development server.

## Notes

- Build artifacts such as `node_modules`, `target`, `dist`, and `build` are excluded from version control.
- The backend and frontend are organized as separate apps inside one clean monorepo root.
