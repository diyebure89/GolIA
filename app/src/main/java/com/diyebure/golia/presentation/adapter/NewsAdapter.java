package com.diyebure.golia.presentation.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.diyebure.golia.R;
import com.diyebure.golia.presentation.ui.news.ArticleUiModel;

import java.text.DateFormat;
import java.util.Date;

/**
 * Adapter de la lista de noticias de la Pantalla_Noticias.
 *
 * <p>Extiende {@link ListAdapter} con {@link NewsDiffCallback} para calcular diffs en un
 * hilo de fondo y animar solo las tarjetas cuyo contenido visible ha cambiado (mismo patrón
 * que {@link MatchesAdapter}).</p>
 *
 * <p>En el bind se muestran (Requisitos 7.1, 7.2, 7.5, 7.6):</p>
 * <ul>
 *     <li>Imagen de fondo cargada con Glide, con placeholder oscuro ante ausencia, fallo o
 *     timeout de 10s (R7.3, R7.4, R13.1).</li>
 *     <li>Badge de liga/categoría (R7.5).</li>
 *     <li>Título ≤ 2 líneas con elipsis (R7.1).</li>
 *     <li>Descripción ≤ 3 líneas con elipsis (R7.1); se oculta si está ausente (R7.2).</li>
 *     <li>Nombre de la fuente ≤ 1 línea con elipsis (R7.1); se oculta si está ausente (R7.2).</li>
 *     <li>Fecha de publicación en formato corto de la Zona_Local (R7.6).</li>
 * </ul>
 *
 * <p><b>IDs de vista esperados en {@code item_news.xml}:</b></p>
 * <ul>
 *     <li>{@code card_news_container} — contenedor raíz clicable.</li>
 *     <li>{@code image_news} — {@link ImageView} de fondo cargado con Glide.</li>
 *     <li>{@code text_badge} — badge de liga/categoría.</li>
 *     <li>{@code text_news_title} — título (maxLines=2, ellipsize=end).</li>
 *     <li>{@code text_news_description} — descripción (maxLines=3, ellipsize=end).</li>
 *     <li>{@code text_news_source} — nombre de la fuente (maxLines=1, ellipsize=end).</li>
 *     <li>{@code text_news_date} — fecha de publicación formateada.</li>
 * </ul>
 */
public class NewsAdapter extends ListAdapter<ArticleUiModel, NewsAdapter.VH> {

    /** Timeout de carga de imagen en milisegundos (R7.4: >10s → placeholder). */
    private static final int IMAGE_TIMEOUT_MS = 10_000;

    /** Duración del crossfade de aparición de la imagen, en milisegundos. */
    private static final int CROSSFADE_MS = 200;

    /**
     * Listener opcional para la pulsación sobre una tarjeta de noticia. El fragment
     * (tarea 13.1) lo usa para abrir la URL del artículo en el navegador externo (R7.7).
     */
    public interface OnArticleClickListener {
        void onArticleClick(@NonNull ArticleUiModel article);
    }

    @Nullable
    private final OnArticleClickListener clickListener;

    public NewsAdapter() {
        this(null);
    }

    public NewsAdapter(@Nullable OnArticleClickListener clickListener) {
        super(new NewsDiffCallback());
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_news, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position), clickListener);
    }

    /**
     * ViewHolder de una tarjeta de noticia.
     */
    static class VH extends RecyclerView.ViewHolder {

        private final View cardContainer;
        private final ImageView newsImage;
        private final TextView badgeText;
        private final TextView titleText;
        private final TextView descriptionText;
        private final TextView sourceText;
        private final TextView dateText;

        VH(@NonNull View itemView) {
            super(itemView);
            cardContainer = itemView.findViewById(R.id.card_news_container);
            newsImage = itemView.findViewById(R.id.image_news);
            badgeText = itemView.findViewById(R.id.text_badge);
            titleText = itemView.findViewById(R.id.text_news_title);
            descriptionText = itemView.findViewById(R.id.text_news_description);
            sourceText = itemView.findViewById(R.id.text_news_source);
            dateText = itemView.findViewById(R.id.text_news_date);
        }

        void bind(@NonNull ArticleUiModel article, @Nullable OnArticleClickListener listener) {
            // Imagen de fondo con Glide + placeholder oscuro (R7.3, R7.4, R13.1).
            loadImage(newsImage, article.imageUrl);

            // Badge de liga/categoría; genérico de fútbol para el feed de Chip_Todos (R7.5).
            bindOptional(badgeText, article.badgeLabel);

            // Título (obligatorio, ≤2 líneas con elipsis por layout) (R7.1). Se oculta solo
            // si viniera vacío para no dejar hueco visible (R7.2).
            bindOptional(titleText, article.title);

            // Descripción (opcional, ≤3 líneas) (R7.1, R7.2).
            bindOptional(descriptionText, article.description);

            // Fuente (opcional, ≤1 línea) (R7.1, R7.2).
            bindOptional(sourceText, article.sourceName);

            // Fecha de publicación en formato corto de la Zona_Local (R7.6).
            bindOptional(dateText, formatPublishedDate(article.publishedAtEpochUtc));

            // Apertura de la URL del artículo, delegada al fragment (R7.7).
            if (cardContainer != null) {
                if (listener != null) {
                    cardContainer.setOnClickListener(v -> listener.onArticleClick(article));
                    cardContainer.setClickable(true);
                } else {
                    cardContainer.setOnClickListener(null);
                    cardContainer.setClickable(false);
                }
            }
        }

        /**
         * Carga la imagen con Glide. Si {@code url} es null/ausente/no cargable o la carga
         * excede 10s, Glide muestra el placeholder oscuro (R7.4). Aplica crossfade,
         * timeout y estrategia de caché en disco automática (R7.3, R13.1).
         */
        private void loadImage(@Nullable ImageView target, @Nullable String url) {
            if (target == null) {
                return;
            }
            Glide.with(target.getContext())
                    .load(url)
                    .placeholder(R.drawable.bg_news_placeholder)
                    .error(R.drawable.bg_news_placeholder)
                    .fallback(R.drawable.bg_news_placeholder)
                    .timeout(IMAGE_TIMEOUT_MS)
                    .transition(DrawableTransitionOptions.withCrossFade(CROSSFADE_MS))
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .centerCrop()
                    .into(target);
        }

        /**
         * Formatea el instante de publicación (epoch UTC en segundos) usando la fecha y hora
         * cortas de la Zona_Local del dispositivo, incluyendo día, mes, año y hora según el
         * formato regional (R7.6). Devuelve null si no hay fecha válida.
         */
        @Nullable
        private static String formatPublishedDate(long epochSeconds) {
            if (epochSeconds <= 0L) {
                return null;
            }
            DateFormat formatter = DateFormat.getDateTimeInstance(
                    DateFormat.SHORT, DateFormat.SHORT);
            return formatter.format(new Date(epochSeconds * 1000L));
        }

        /**
         * Fija el texto y muestra la vista si {@code value} no está vacío; en caso contrario
         * la oculta con {@link View#GONE} para no dejar espacio en blanco visible (R7.2).
         */
        private static void bindOptional(@Nullable TextView view, @Nullable String value) {
            if (view == null) {
                return;
            }
            if (TextUtils.isEmpty(value)) {
                view.setVisibility(View.GONE);
                view.setText("");
            } else {
                view.setVisibility(View.VISIBLE);
                view.setText(value);
            }
        }
    }
}
