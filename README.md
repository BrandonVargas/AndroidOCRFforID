# AndroidOCRFforID
Implementación de Mobile Vision en Android para extraer información de identificaciones mexicanas (IFE/INE) 

# Configuración
File -> New -> Import Module,seleccionar el modulo OCR de este proyecto, se agrega automaticamente uCrop

File -> Project Structure -> Modules/app -> Pestaña Dependencias -> + -> Module dependency -> :ocr

Añadir al Manifest 
```xml
<activity
    android:name="mx.brandonvargas.ocrforid.OcrIdActivity"
    android:screenOrientation="portrait"
    android:theme="@style/AppTheme.NoActionBar"
    />
<activity
    android:name="com.yalantis.ucrop.UCropActivity"
    android:screenOrientation="portrait"
    android:theme="@style/AppTheme.NoActionBar"
    />
```
Y añadir a styles
```xml
<style name="AppTheme.NoActionBar">
    <item name="windowActionBar">false</item>
    <item name="windowNoTitle">true</item>
    <item name="android:windowBackground">@android:color/white</item>
    <item name="android:windowDrawsSystemBarBackgrounds">true</item>
    <item name="android:statusBarColor">@color/colorPrimary</item>
</style>
```
# Obteniendo la información

Para iniciar el OCR
```java
startActivityForResult(new Intent(MainActivity.this, OcrIdActivity.class),OCR_REQUEST);
```

Para recuperar los datos
```java
 @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bundle extras = data.getExtras();
        if(resultCode == Activity.RESULT_OK && requestCode == OCR_REQUEST && extras!=null){
            Boolean is_ine = extras.getBoolean("IS_INE");
            String name = extras.getString("NAME");
            String lastName = extras.getString("LAST_NAME");
            String motherLastName = extras.getString("M_LAST_NAME");
            String curp = extras.getString("CURP");
            String address = extras.getString("ADDRESS");
            String elector = extras.getString("ELECTOR");
            String state = extras.getString("STATE");
            String town = extras.getString("TOWN");
            String section = extras.getString("SECTION");
            String id = extras.getString("ID");
            Uri uri1 = Uri.parse(extras.getString("URI1"));
            Uri uri2 = Uri.parse(extras.getString("URI2"));           
        }
    }
```

# To Do
* Mejorar Algoritmos de detección
* Mejorar Algortimo para tomar foto y recortar
* Resolver problemas con smartphones Samsung

# Librerias de terceros
* A old version of uCrop https://github.com/Yalantis/uCrop
* Mobile Vision https://developers.google.com/vision/

# Licencia/License
Copyright [2017] [Brandon Vargas]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
