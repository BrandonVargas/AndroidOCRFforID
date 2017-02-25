# AndroidOCRFforID
Implementación de Mobile Vision en Android para extraer información de identificaciones mexicanas (IFE/INE) 

# Configuración
File -> New -> Import Module,seleccionar el modulo OCR de este proyecto, se agrega automaticamente uCrop

File -> Project Structure -> Modules/app -> Pestaña Dependencias -> + -> Module dependency -> :ocr

Añadir al Manifest 
<code>
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
</code>
