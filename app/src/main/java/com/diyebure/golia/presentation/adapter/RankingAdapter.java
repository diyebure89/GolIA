package com.diyebure.golia.presentation.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.diyebure.golia.R;
import com.diyebure.golia.domain.model.Ranking_Entry;

import java.io.File;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Adapter de la lista de posiciones de la Pantalla_Ranking (ranking-screen, tarea 8.2).
 *
 * <p>Extiende {@link ListAdapter} con {@link RankingDiffCallback} para calcular diffs en un
 * hilo de fondo y re-ligar solo las filas cuyo contenido visible ha cambiado (mismo patrón
 * que {@link MatchesAdapter} y {@link NewsAdapter}), útil para refrescar los puntos en vivo
 * sin parpadeos.</p>
 *
 * <p>Cada fila enlaza un {@link Ranking_Entry} y muestra (Requisitos 5.1, 5.4, 6.5, 2.5):</p>
 * <ul>
 *     <li>Medalla para los tres primeros puestos o el número de posición para el resto.</li>
 *     <li>Avatar del participante; se degrada al placeholder cuando el {@code avatarRef} es
 *     nulo, apunta a un archivo inexistente o a un recurso no resoluble (R5.4).</li>
 *     <li>Nombre del participante.</li>
 *     <li>Línea de métricas "% acierto · Racha: N" (R5.1).</li>
 *     <li>Puntos formateados con el separador de miles de la región del dispositivo, p. ej.
 *     "3,420 pts" (R5.4).</li>
 *     <li>Distintivo "en vivo" cuando el participante aporta puntos en vivo (R6.5).</li>
 * </ul>
 *
 * <p>La fila del {@code Current_User} se resalta cambiando el fondo del contenedor raíz
 * (R2.5).</p>
 *
 * <p><b>IDs de vista esperados en {@code item_ranking.xml}:</b></p>
 * <ul>
 *     <li>{@code row_ranking_container} — contenedor raíz; se resalta para el usuario actual.</li>
 *     <li>{@code text_position} — medalla o número de posición.</li>
 *     <li>{@code image_avatar} — avatar del participante (o placeholder).</li>
 *     <li>{@code text_name} — nombre del participante.</li>
 *     <li>{@code text_metrics} — "% acierto · Racha: N".</li>
 *     <li>{@code text_live_badge} — distintivo "en vivo"; visible solo con contribución en vivo.</li>
 *     <li>{@code text_points} — puntos con separador de miles regional.</li>
 * </ul>
 */
public class RankingAdapter extends ListAdapter<Ranking_Entry, RankingAdapter.VH> {

    /** Medallas para los tres primeros puestos del ranking. */
    private static final String MEDAL_GOLD = "\uD83E\uDD47";   // 1.º
    private static final String MEDAL_SILVER = "\uD83E\uDD48"; // 2.º
    private static final String MEDAL_BRONZE = "\uD83E\uDD49"; // 3.º

    public RankingAdapter() {
        super(new RankingDiffCallback());
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ranking, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position));
    }

    /**
     * ViewHolder de una fila del ranking.
     */
    static class VH extends RecyclerView.ViewHolder {

        private final View rowContainer;
        private final TextView positionText;
        private final ImageView avatarImage;
        private final TextView nameText;
        private final TextView metricsText;
        private final TextView liveBadge;
        private final TextView pointsText;

        VH(@NonNull View itemView) {
            super(itemView);
            rowContainer = itemView.findViewById(R.id.row_ranking_container);
            positionText = itemView.findViewById(R.id.text_position);
            avatarImage = itemView.findViewById(R.id.image_avatar);
            nameText = itemView.findViewById(R.id.text_name);
            metricsText = itemView.findViewById(R.id.text_metrics);
            liveBadge = itemView.findViewById(R.id.text_live_badge);
            pointsText = itemView.findViewById(R.id.text_points);
        }

        void bind(@NonNull Ranking_Entry entry) {
            Context context = itemView.getContext();

            // Medalla para el podio; número para el resto de posiciones (R5.1).
            positionText.setText(positionLabel(entry.getPosition()));

            // Nombre del participante (R5.1).
            nameText.setText(entry.getDisplayName());

            // Métricas "% acierto · Racha: N" (R5.1).
            metricsText.setText(context.getString(
                    R.string.ranking_metricas_formato,
                    entry.getWinPercentage(),
                    entry.getStreak()));

            // Puntos con separador de miles regional: "3,420 pts" (R5.4).
            String formattedPoints = NumberFormat.getIntegerInstance(Locale.getDefault())
                    .format(entry.getPoints());
            pointsText.setText(context.getString(
                    R.string.ranking_puntos_formato, formattedPoints));

            // Distintivo "en vivo" solo cuando el participante aporta puntos en vivo (R6.5).
            liveBadge.setVisibility(entry.hasLive() ? View.VISIBLE : View.GONE);

            // Resaltado de la fila del usuario actual (R2.5).
            rowContainer.setBackgroundResource(entry.isCurrentUser()
                    ? R.drawable.bg_ranking_row_current_user
                    : R.drawable.bg_match_card);

            // Avatar; se degrada a placeholder si es nulo/inexistente/no resoluble (R5.4).
            bindAvatar(avatarImage, entry.getAvatarRef());
        }

        /**
         * Devuelve la medalla del podio para los puestos 1-3 o el número de posición como
         * texto para el resto.
         */
        private static String positionLabel(int position) {
            switch (position) {
                case 1:
                    return MEDAL_GOLD;
                case 2:
                    return MEDAL_SILVER;
                case 3:
                    return MEDAL_BRONZE;
                default:
                    return String.valueOf(position);
            }
        }

        /**
         * Resuelve el {@code avatarRef} y lo carga en {@code target}, degradando al
         * placeholder cuando (R5.4):
         * <ul>
         *     <li>{@code avatarRef} es nulo o vacío;</li>
         *     <li>apunta a un archivo local que no existe o no puede decodificarse (avatar
         *     del usuario real, guardado como ruta absoluta);</li>
         *     <li>apunta al nombre de un recurso drawable que no puede resolverse (perfiles
         *     seed).</li>
         * </ul>
         */
        private static void bindAvatar(@NonNull ImageView target, @Nullable String avatarRef) {
            if (TextUtils.isEmpty(avatarRef)) {
                target.setImageResource(R.drawable.ic_avatar_placeholder);
                return;
            }

            // 1) Ruta de archivo absoluta (avatar del usuario real, PhotoStorage).
            File file = new File(avatarRef);
            if (file.isAbsolute() && file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(avatarRef);
                if (bitmap != null) {
                    target.setImageBitmap(bitmap);
                    return;
                }
            }

            // 2) Nombre de recurso drawable (perfiles seed, p. ej. "ic_avatar_placeholder").
            Context context = target.getContext();
            int resId = context.getResources().getIdentifier(
                    avatarRef, "drawable", context.getPackageName());
            if (resId != 0) {
                target.setImageResource(resId);
                return;
            }

            // 3) Degradación: placeholder por defecto.
            target.setImageResource(R.drawable.ic_avatar_placeholder);
        }
    }
}
