# Stockfish Setup Guide

## **Overview**
This project uses **Stockfish**, a powerful open-source chess engine, to evaluate chess positions. However, **Stockfish is not included by default** and must be downloaded before running the application.

## **Installation Instructions**
To ensure Stockfish is properly installed and ready for use, you must run the **`download_stockfish.py`** script before starting the application.

### **Step 1: Run the Stockfish Download Script**
Execute the following command in your terminal or command prompt:
```bash
python download_stockfish.py
```
This script will:
- **Download the latest Stockfish engine** from the official repository.
- **Extract only the necessary executable file** (`stockfish.exe` for Windows, or `stockfish` for Linux/macOS).
- **Place the executable inside the `engines/` directory** for easy access.

### **Step 2: Verify Stockfish Installation**
Once the script completes, check that the `engines/` directory contains:
```
engines/
    stockfish.exe
```
If the file is missing, ensure the download script ran successfully and manually verify the download.

### **Step 3: Run the Application**
Once Stockfish is installed, you can proceed with running the main application as usual.
## **Final Notes**
This step is required **only once**, unless you want to update Stockfish to a newer version. Run the script again whenever you need to update Stockfish.