import os

res_dir = r"d:\VS Code\Android\Back-tap\app\src\main\res"

files = {}

files[r"layout\widget_back_tap_toggle.xml"] = """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/widget_container"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/bg_widget_rect"
    android:gravity="center"
    android:orientation="horizontal"
    android:paddingStart="16dp"
    android:paddingEnd="16dp"
    android:paddingTop="16dp"
    android:paddingBottom="16dp">
    <TextView
        android:id="@+id/widget_text"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:gravity="start|center_vertical"
        android:text="Back Tap"
        android:textColor="@android:color/white"
        android:textSize="14sp"
        android:textStyle="bold"
        android:maxLines="1"
        android:ellipsize="end"
        android:fontFamily="sans-serif-medium" />
    <ImageView
        android:id="@+id/widget_switch"
        android:layout_width="52dp"
        android:layout_height="32dp"
        android:layout_marginStart="16dp"
        android:src="@drawable/ic_switch_off" />
</LinearLayout>"""

files[r"drawable\bg_widget_rect.xml"] = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#99000000" /> <!-- Translucent pitch black -->
    <corners android:radius="16dp" />
    <stroke android:width="1dp" android:color="#33FFFFFF" />
</shape>"""

files[r"xml\back_tap_widget_info.xml"] = """<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:initialLayout="@layout/widget_back_tap_toggle"
    android:minWidth="110dp"
    android:minHeight="40dp"
    android:previewImage="@mipmap/ic_launcher"
    android:previewLayout="@layout/widget_back_tap_toggle"
    android:description="@string/app_name"
    android:widgetFeatures="reconfigurable|configuration_optional"
    android:resizeMode="horizontal|vertical"
    android:updatePeriodMillis="0"
    android:widgetCategory="home_screen" />"""

files[r"drawable\bg_widget_oval.xml"] = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="#0F1012" />
    <stroke android:width="2dp" android:color="#1A1C1F" />
</shape>"""

files[r"drawable\bg_widget_pill.xml"] = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#26282B" />
    <stroke android:width="2dp" android:color="#000000" />
    <corners android:radius="100dp" />
</shape>"""

files[r"drawable\ic_switch_off.xml"] = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="52dp"
    android:height="32dp"
    android:viewportWidth="52"
    android:viewportHeight="32">
    <!-- Dark gray track -->
    <path
        android:fillColor="#39393D"
        android:pathData="M16,0 L36,0 A16,16 0 0 1 52,16 A16,16 0 0 1 36,32 L16,32 A16,16 0 0 1 0,16 A16,16 0 0 1 16,0 Z" />
    <!-- White thumb on left -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M16,2 A14,14 0 1 1 16,30 A14,14 0 1 1 16,2 Z" />
</vector>"""

files[r"drawable\ic_switch_on.xml"] = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="52dp"
    android:height="32dp"
    android:viewportWidth="52"
    android:viewportHeight="32">
    <!-- Green track -->
    <path
        android:fillColor="#34C759"
        android:pathData="M16,0 L36,0 A16,16 0 0 1 52,16 A16,16 0 0 1 36,32 L16,32 A16,16 0 0 1 0,16 A16,16 0 0 1 16,0 Z" />
    <!-- White thumb on right -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M36,2 A14,14 0 1 1 36,30 A14,14 0 1 1 36,2 Z" />
</vector>"""

files[r"drawable\ic_widget_toggle_off.xml"] = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp"
    android:height="48dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <!-- Dark Circle Background with dark stroke -->
    <path
        android:fillColor="#151719"
        android:strokeColor="#0A0B0C"
        android:strokeWidth="1.5"
        android:pathData="M12,2 A10,10 0 1 1 12,22 A10,10 0 1 1 12,2 Z" />
    <!-- U Magnet (Grayed out) -->
    <path
        android:fillColor="#B0B0B0"
        android:pathData="M7,8 L7,15 A5,5 0 0 0 17,15 L17,8 L13.5,8 L13.5,15 A1.5,1.5 0 0 1 10.5,15 L10.5,8 Z" />
    <!-- Left Plus -->
    <path
        android:fillColor="#B0B0B0"
        android:pathData="M3,11.5 L6,11.5 L6,12.5 L3,12.5 Z M4.5,10 L4.5,14 L3.5,14 L3.5,10 Z" />
    <!-- Right Plus -->
    <path
        android:fillColor="#B0B0B0"
        android:pathData="M18,11.5 L21,11.5 L21,12.5 L18,12.5 Z M19.5,10 L19.5,14 L18.5,14 L18.5,10 Z" />
    <!-- Slash for OFF state -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M4.5,20.5 L19.5,3.5 L20.5,4.5 L5.5,21.5 Z" />
</vector>"""

files[r"drawable\ic_widget_toggle_on.xml"] = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp"
    android:height="48dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <!-- Green Circle Background with dark stroke -->
    <path
        android:fillColor="#4F7928"
        android:strokeColor="#1A1C1F"
        android:strokeWidth="1.5"
        android:pathData="M12,2 A10,10 0 1 1 12,22 A10,10 0 1 1 12,2 Z" />
    <!-- U Magnet -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M7,8 L7,15 A5,5 0 0 0 17,15 L17,8 L13.5,8 L13.5,15 A1.5,1.5 0 0 1 10.5,15 L10.5,8 Z" />
    <!-- Left Plus -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M3,11.5 L6,11.5 L6,12.5 L3,12.5 Z M4.5,10 L4.5,14 L3.5,14 L3.5,10 Z" />
    <!-- Right Plus -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M18,11.5 L21,11.5 L21,12.5 L18,12.5 Z M19.5,10 L19.5,14 L18.5,14 L18.5,10 Z" />
</vector>"""

for rel_path, content in files.items():
    full_path = os.path.join(res_dir, rel_path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, "w", encoding="utf-8") as f:
        f.write(content)
