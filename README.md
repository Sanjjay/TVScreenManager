<p align="center">
<img src="https://github.com/user-attachments/assets/fe4d8a63-041a-4b79-93c6-05bf974d4639" width="320" alt="tv_banner" />
</p>

# TVScreenManager 📺

**TVScreenManager** is a utility tool designed for Android TV, Google TV, and Amazon Fire TV devices. It gives you full control over your TV's display habits, allowing you to bypass strict system limitations and customize your screen's behavior exactly how you want it.

---
## 📸 Screenshot

<p align="center">
  <img src="https://github.com/user-attachments/assets/f4a29f24-9640-4a7a-baa5-3b8f46571b08" width="700" alt="screenshot" />
</p>

---

## ✨ Features

* **Screen-Off Timeouts:** Easily adjust how long your TV stays idle before the screen turns off completely.
* **Sleep Timeouts:** Customize exactly when your device goes into a deep sleep mode to save energy.
* **Custom Daydream Selection:** Set *any* installed screensaver app (Daydream) as your default, even if your TV's system settings try to block or hide it.
* **Custom Keyboard Selection:** Set *any* installed keyboard app as your default, even if your TV's system settings try to block or hide it.
* **Fire TV & Google TV Compatible:** Works seamlessly across various TV operating systems that traditionally restrict customization.

---

## 🚀 Setup Instructions

Because changing keyboard, screensaver and timeout settings requires deeper system access, Android requires you to grant a special permission called `WRITE_SECURE_SETTINGS`. 

You only need to do this **once** using a computer and an ADB (Android Debug Bridge) command.

📋 Prerequisite: Install ADB
You must have ADB installed on your computer before running commands. 

* **Windows:** Download [Platform-Tools](https://dl.google.com/android/repository/platform-tools-latest-windows.zip), extract the zip, and open Command Prompt inside that folder.
* **Mac:** Run `brew install android-platform-tools` in Terminal.
* **Linux:** Run `sudo apt install adb` (Ubuntu) or `sudo dnf install android-tools` (Fedora).

##

### Step 1: Enable ADB Debugging on your TV
1. Open your TV **Settings** and go to **System** > **About**.
2. Scroll down to **Build** (or **OS Build**) and click it **7 times** until it says "You are now a developer."
3. Go back to the previous menu, open **Developer Options**, and turn on **ADB Debugging**.
4. Note down your TV's **IP Address** (usually found in Settings > Network & Internet).

### Step 2: Connect via your Computer, both devices has to be on the same wireless network
Open a Terminal (Mac/Linux) or Command Prompt (Windows) on your computer and run the following commands to connect to your TV:

```bash
# Connect to your TV (Replace with your actual TV IP address)
adb connect 192.168.1.X

# Verify you are connected (You should see your TV listed as 'device')
adb devices
```
*(Note: A popup will appear on your TV screen asking to allow USB/ADB debugging. Click **Allow** or **Always Allow**).*

### Step 3: Grant Secure Permissions
Copy and paste the exact command below into your computer's terminal to give **TVScreenManager** the power to change your screensaver and keyboard:

```bash
adb shell pm grant com.tvscreenmanager android.permission.WRITE_SECURE_SETTINGS
```
---

## 🛠️ How It Works

Android TVs use "Secure Settings" to control what happens when the device is idle. Applications cannot change these settings by default to prevent malicious apps from taking over your screen. 

By running the ADB command above, you unlock the doors for **TVScreenManager** to safely talk to the Android system system and write your preferred choices directly into the TV's configuration file.

---

## 📜 License

This project is licensed under the **GNU Affero General Public License v3.0 (AGPL-3.0)**. 

As part of the AGPL-3.0 terms, if you modify this software and host it over a network for other users, you must make your modified source code publicly available. See the [LICENSE](LICENSE) file for full details.
