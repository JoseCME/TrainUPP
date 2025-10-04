package com.jossecm.myapplication.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import com.jossecm.myapplication.OnboardingActivity;
import com.jossecm.myapplication.R;
import com.jossecm.myapplication.models.User;
import com.jossecm.myapplication.repository.FitnessRepository;
import com.jossecm.myapplication.utils.ImageUtils;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PerfilFragment extends Fragment {

    private LinearLayout layoutLoading, layoutContent;
    private ImageView imageViewProfile;
    private TextView textViewNombre;
    private TextView textViewEdad;
    private TextView textViewGenero;
    private TextView textViewPeso;
    private TextView textViewAltura;
    private TextView textViewNivelFitness;
    private TextView textViewObjetivo;
    private TextView textViewEquipamiento;
    private TextView textViewLesiones;
    private TextView textViewPreferenciaIA;
    private TextView textViewFechaRegistro;
    private Button buttonEditarPerfil;

    private FitnessRepository repository;
    private User currentUser;

    // URI temporal para la foto tomada con la cámara
    private Uri photoUri;

    // Activity Result Launchers para permisos e imágenes
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<String> requestStoragePermissionLauncher;
    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    public static PerfilFragment newInstance() {
        return new PerfilFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeActivityResultLaunchers();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        initViews(view);
        setupButtons();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
    }

    private void initializeActivityResultLaunchers() {
        // Launcher para permiso de cámara
        requestCameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(getContext(), R.string.camera_permission_required, Toast.LENGTH_SHORT).show();
                }
            }
        );

        // Launcher para permiso de almacenamiento
        requestStoragePermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openGallery();
                } else {
                    Toast.makeText(getContext(), R.string.storage_permission_required, Toast.LENGTH_SHORT).show();
                }
            }
        );

        // Launcher para tomar foto
        takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK) {
                    if (photoUri != null) {
                        handleImageSelection(photoUri);
                    }
                }
            }
        );

        // Launcher para seleccionar de galería
        pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        handleImageSelection(selectedImageUri);
                    }
                }
            }
        );
    }

    private void initViews(View view) {
        layoutLoading = view.findViewById(R.id.layoutLoading);
        layoutContent = view.findViewById(R.id.layoutContent);

        imageViewProfile = view.findViewById(R.id.imageViewProfile);
        textViewNombre = view.findViewById(R.id.textViewNombre);
        textViewEdad = view.findViewById(R.id.textViewEdad);
        textViewGenero = view.findViewById(R.id.textViewGenero);
        textViewPeso = view.findViewById(R.id.textViewPeso);
        textViewAltura = view.findViewById(R.id.textViewAltura);
        textViewNivelFitness = view.findViewById(R.id.textViewNivelFitness);
        textViewObjetivo = view.findViewById(R.id.textViewObjetivo);
        textViewEquipamiento = view.findViewById(R.id.textViewEquipamiento);
        textViewLesiones = view.findViewById(R.id.textViewLesiones);
        textViewPreferenciaIA = view.findViewById(R.id.textViewPreferenciaIA);
        textViewFechaRegistro = view.findViewById(R.id.textViewFechaRegistro);
        buttonEditarPerfil = view.findViewById(R.id.buttonEditarPerfil);

        repository = new FitnessRepository(requireContext());
    }

    private void setupButtons() {
        buttonEditarPerfil.setOnClickListener(v -> {
            // Navegar al onboarding para editar perfil
            Intent intent = new Intent(getActivity(), OnboardingActivity.class);
            intent.putExtra("edit_mode", true);
            startActivity(intent);
        });

        // Click en imagen de perfil para cambiarla
        imageViewProfile.setOnClickListener(v -> showImageSelectionDialog());
    }

    private void showImageSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.select_profile_image);

        String[] options = {
            getString(R.string.choose_from_gallery),
            getString(R.string.take_photo),
            getString(R.string.cancel)
        };

        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Galería
                    checkStoragePermissionAndOpenGallery();
                    break;
                case 1: // Cámara
                    checkCameraPermissionAndOpenCamera();
                    break;
                case 2: // Cancelar
                    dialog.dismiss();
                    break;
            }
        });

        builder.show();
    }

    private void checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void checkStoragePermissionAndOpenGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ usa READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                requestStoragePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            // Android 12 y anteriores
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                requestStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Crear archivo temporal para la foto
        File photoFile = createImageFile();
        if (photoFile != null) {
            photoUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                photoFile
            );
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            takePictureLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(getContext(), R.string.error_loading_image, Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(pickPhotoIntent);
    }

    private File createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = requireContext().getCacheDir();
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void handleImageSelection(Uri imageUri) {
        // Convertir imagen a byte array
        byte[] imageBytes = ImageUtils.uriToByteArray(requireContext(), imageUri);

        if (imageBytes != null && currentUser != null) {
            // Actualizar imagen en el objeto user
            currentUser.setProfileImage(imageBytes);

            // Guardar en la base de datos
            repository.updateUser(currentUser, new FitnessRepository.OperationCallback() {
                @Override
                public void onSuccess() {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            // Mostrar la imagen en el ImageView
                            Bitmap bitmap = ImageUtils.byteArrayToBitmap(imageBytes);
                            imageViewProfile.setImageBitmap(bitmap);
                            Toast.makeText(getContext(), R.string.profile_image_updated, Toast.LENGTH_SHORT).show();
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), R.string.error_loading_image + ": " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        } else {
            Toast.makeText(getContext(), R.string.error_loading_image, Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserData() {
        showLoading();

        repository.getCurrentUser(new FitnessRepository.DataCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (user != null) {
                            currentUser = user;
                            displayUserData();
                            showContent();
                        } else {
                            Toast.makeText(getContext(), "No se encontraron datos de usuario", Toast.LENGTH_SHORT).show();
                            showLoading();
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        android.util.Log.e("PerfilFragment", "Error cargando usuario: " + error);
                        Toast.makeText(getContext(), "Error cargando perfil: " + error, Toast.LENGTH_LONG).show();
                        showLoading();
                    });
                }
            }
        });
    }

    private void displayUserData() {
        // Cargar imagen de perfil
        if (currentUser.getProfileImage() != null && currentUser.getProfileImage().length > 0) {
            Bitmap bitmap = ImageUtils.byteArrayToBitmap(currentUser.getProfileImage());
            imageViewProfile.setImageBitmap(bitmap);
        } else {
            // Mostrar ícono por defecto
            imageViewProfile.setImageResource(R.drawable.ic_profile_placeholder);
        }

        // Datos personales
        textViewNombre.setText(currentUser.getName() != null ? currentUser.getName() : "No especificado");
        textViewEdad.setText(currentUser.getAge() > 0 ? currentUser.getAge() + " años" : "No especificado");
        textViewGenero.setText(currentUser.getGender() != null ? currentUser.getGender() : "No especificado");

        // Datos físicos
        textViewPeso.setText(currentUser.getWeight() > 0 ?
            String.format(Locale.getDefault(), "%.1f kg", currentUser.getWeight()) : "No especificado");
        textViewAltura.setText(currentUser.getHeight() > 0 ?
            String.format(Locale.getDefault(), "%.0f cm", currentUser.getHeight()) : "No especificado");

        // Datos de fitness
        textViewNivelFitness.setText(currentUser.getFitnessLevel() != null ?
            currentUser.getFitnessLevel() : "No especificado");
        textViewObjetivo.setText(currentUser.getGoal() != null ?
            currentUser.getGoal() : "No especificado");

        // Equipamiento (mostrar cantidad)
        if (currentUser.getSelectedEquipmentIds() != null && !currentUser.getSelectedEquipmentIds().isEmpty()) {
            textViewEquipamiento.setText(currentUser.getSelectedEquipmentIds().size() + " equipos seleccionados");
        } else {
            textViewEquipamiento.setText("Ningún equipo seleccionado");
        }

        // Lesiones
        if (currentUser.getInjuries() != null && !currentUser.getInjuries().isEmpty()) {
            textViewLesiones.setText("Tiene lesiones registradas");
        } else {
            textViewLesiones.setText("Sin lesiones registradas");
        }

        // Preferencia de IA
        textViewPreferenciaIA.setText(currentUser.isUseAI() ? "Habilitada" : "Deshabilitada");

        // Fecha de registro (si tienes este campo en User)
        textViewFechaRegistro.setText("Usuario registrado");
    }

    private void showLoading() {
        layoutLoading.setVisibility(View.VISIBLE);
        layoutContent.setVisibility(View.GONE);
    }

    private void showContent() {
        layoutLoading.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);
    }
}
