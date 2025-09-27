package com.jossecm.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.jossecm.myapplication.R;
import com.jossecm.myapplication.models.Mensaje;
import java.util.List;

public class MensajeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int MESSAGE_USER = 1;
    private static final int MESSAGE_IA = 2;

    private List<Mensaje> mensajes;

    public MensajeAdapter(List<Mensaje> mensajes) {
        this.mensajes = mensajes;
    }

    @Override
    public int getItemViewType(int position) {
        Mensaje mensaje = mensajes.get(position);
        return mensaje.isEsUsuario() ? MESSAGE_USER : MESSAGE_IA;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == MESSAGE_USER) {
            View view = inflater.inflate(R.layout.message_user, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.message_ia, parent, false);
            return new IAMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Mensaje mensaje = mensajes.get(position);

        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).bind(mensaje);
        } else if (holder instanceof IAMessageViewHolder) {
            ((IAMessageViewHolder) holder).bind(mensaje);
        }
    }

    @Override
    public int getItemCount() {
        return mensajes.size();
    }

    // Métodos para manejar mensajes dinámicamente
    public void agregarMensaje(Mensaje mensaje) {
        mensajes.add(mensaje);
        notifyItemInserted(mensajes.size() - 1);
    }

    public void actualizarMensaje(int position, Mensaje mensaje) {
        if (position >= 0 && position < mensajes.size()) {
            mensajes.set(position, mensaje);
            notifyItemChanged(position);
        }
    }

    public void limpiarMensajes() {
        int size = mensajes.size();
        mensajes.clear();
        notifyItemRangeRemoved(0, size);
    }

    // ViewHolder para mensajes del usuario
    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMensajeTexto;
        private TextView tvTiempo;
        private ProgressBar progressEnviando;

        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMensajeTexto = itemView.findViewById(R.id.tvMensajeTexto);
            tvTiempo = itemView.findViewById(R.id.tvTiempo);
            progressEnviando = itemView.findViewById(R.id.progressEnviando);
        }

        public void bind(Mensaje mensaje) {
            tvMensajeTexto.setText(mensaje.getTexto());
            tvTiempo.setText(mensaje.getTimeFormatted());

            // Mostrar/ocultar indicador de envío
            if (mensaje.isEnviando()) {
                progressEnviando.setVisibility(View.VISIBLE);
                tvTiempo.setVisibility(View.GONE);
            } else {
                progressEnviando.setVisibility(View.GONE);
                tvTiempo.setVisibility(View.VISIBLE);
            }
        }
    }

    // ViewHolder para mensajes de la IA
    static class IAMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMensajeTexto;
        private TextView tvTiempo;
        private ImageView ivAvatarIA;

        public IAMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMensajeTexto = itemView.findViewById(R.id.tvMensajeTexto);
            tvTiempo = itemView.findViewById(R.id.tvTiempo);
            ivAvatarIA = itemView.findViewById(R.id.ivAvatarIA);
        }

        public void bind(Mensaje mensaje) {
            tvMensajeTexto.setText(mensaje.getTexto());
            tvTiempo.setText(mensaje.getTimeFormatted());
        }
    }
}
