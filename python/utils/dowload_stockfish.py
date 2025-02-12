import os
import requests
import zipfile
import shutil

STOCKFISH_URL = "https://github.com/official-stockfish/Stockfish/releases/latest/download/stockfish-windows-x86-64-avx2.zip"
DEST_DIR = "../engines"
STOCKFISH_EXE_NAME = "stockfish-windows-x86-64-avx2.exe"
FINAL_EXE_NAME = "stockfish.exe"

def download_stockfish():
    if not os.path.exists(DEST_DIR):
        os.makedirs(DEST_DIR)

    zip_path = os.path.join(DEST_DIR, "stockfish.zip")

    print("Downloading Stockfish...")
    response = requests.get(STOCKFISH_URL, stream=True)

    if response.status_code != 200:
        print("Error: Failed to download Stockfish.")
        return

    with open(zip_path, "wb") as file:
        for chunk in response.iter_content(1024):
            file.write(chunk)

    print("Download complete. Verifying file...")

    if not zipfile.is_zipfile(zip_path):
        print("Error: Downloaded file is not a valid ZIP archive.")
        os.remove(zip_path)
        return

    print("Extracting Stockfish...")
    with zipfile.ZipFile(zip_path, "r") as zip_ref:
        extracted_folder = os.path.commonprefix(zip_ref.namelist())
        zip_ref.extractall(DEST_DIR)

    os.remove(zip_path)

    extracted_exe_path = os.path.join(DEST_DIR, extracted_folder, STOCKFISH_EXE_NAME)
    final_exe_path = os.path.join(DEST_DIR, FINAL_EXE_NAME)

    if os.path.exists(extracted_exe_path):
        shutil.move(extracted_exe_path, final_exe_path)
        print(f"Stockfish is ready! Executable saved as {FINAL_EXE_NAME} in {DEST_DIR}")
    else:
        print("Error: Stockfish executable not found after extraction.")
        return

    # Remove the now-empty extracted subdirectory
    extracted_folder_path = os.path.join(DEST_DIR, extracted_folder)
    if os.path.exists(extracted_folder_path) and os.path.isdir(extracted_folder_path):
        shutil.rmtree(extracted_folder_path)

if __name__ == "__main__":
    download_stockfish()
