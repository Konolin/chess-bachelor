# Chess Project

A simple chess application with a Spring Boot backend and a JavaScript frontend.

## Table of Contents

* [Prerequisites](#prerequisites)
* [Installation](#installation)
* [Running the Application](#running-the-application)
* [How to Play](#how-to-play)
* [Restarting the Game](#restarting-the-game)

## Prerequisites

Before you begin, ensure you have met the following requirements:

* **Java 22** or higher installed and configured in your `PATH`.
* **Maven** installed and configured in your `PATH`.
* **Node.js** (v14+) and **npm** installed.

## Installation

1. **Clone the repository**

   ```bash
   git clone <repo-url>
   cd <your-repo-directory>
   ```

2. **Setup the backend**

   ```bash
   cd backend
   mvn clean install
   ```

3. **Setup the frontend**

   ```bash
   cd ../frontend
   npm install
   ```

## Running the Application

1. **Start the backend Spring Boot server**

   ```bash
   cd backend
   mvn spring-boot:run
   ```

   The backend will start on port `8080` by default.

2. **Start the frontend**

   ```bash
   cd ../frontend
   npm start
   ```

   This will launch the frontend on port `4200`.

3. **Access the application**

   Open your browser and navigate to:

   ```
   http://localhost:4200
   ```

## How to Play

1. **Select a piece** on the board by clicking it.
2. **Select a valid square** marked by a highlighted circle.
3. After your move, **wait for the computer** opponent to make its move.
4. Repeat steps 1–3 until the game ends.

## Restarting the Game

To start a new game at any time, simply **refresh the browser page**.
