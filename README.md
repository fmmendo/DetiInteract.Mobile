# DetiInteract.Mobile
Mobile client for the kiosk app developed for my MSc

This is and android app the connects to the main [WPF kiosk app](https://github.com/fmmendo/DetiInteract).
It app streams data over bluetooth which encodes common gestures (scroll, pinch zoom, gyro data) and each page responds appropriately.

Kios app contains:
- List of teachers: Allows the user to view a list of all teachers in the department, and check out their webpage.
- Schedule: Fetches all the classes in the department and displays them in a schedule, grouped by degree and year.
- Google Earth map: Lets the user pan and zoom.
- XNA 3d Model viewer: Loads a 3D model and locks it to the connected phone's gyro
