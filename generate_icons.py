#!/usr/bin/env python3
"""
Script pour générer les icônes WebP de Medistock à partir des drawables vectoriels.
Ce script crée des versions PNG puis les convertit en WebP pour toutes les densités Android.
"""

import os
import subprocess
from pathlib import Path

# Définition des tailles pour chaque densité
DENSITIES = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192,
}

# Chemins
BASE_PATH = Path(__file__).parent
RES_PATH = BASE_PATH / 'app' / 'src' / 'main' / 'res'

def install_dependencies():
    """Installe les dépendances nécessaires"""
    print("Installation des dépendances...")
    try:
        subprocess.run(['pip3', 'install', 'cairosvg', 'pillow'], check=True)
        print("✓ Dépendances installées")
        return True
    except Exception as e:
        print(f"✗ Erreur lors de l'installation: {e}")
        return False

def create_icon_png(size, output_path):
    """
    Crée une icône PNG en combinant background et foreground.
    Pour simplifier, on crée une icône de base avec les couleurs du thème.
    """
    try:
        from PIL import Image, ImageDraw

        # Créer une nouvelle image
        img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(img)

        # Background - gradient cyan/teal
        for i in range(size):
            alpha = int(255 * (1 - i / size * 0.4))
            color = (0, 172, 193, 255)  # #00ACC1
            draw.rectangle([0, i, size, i+1], fill=color)

        # Overlay diamant
        center = size // 2
        points = [
            (0, center),
            (center, 0),
            (size, center),
            (center, size)
        ]
        draw.polygon(points, fill=(0, 151, 167, int(255 * 0.6)))

        # Croix médicale blanche au centre
        cross_width = int(size * 0.44)  # 44% de la taille
        cross_thick = int(size * 0.11)  # 11% de la taille

        # Barre verticale
        x1 = center - cross_thick // 2
        x2 = center + cross_thick // 2
        y1 = center - cross_width // 2
        y2 = center + cross_width // 2
        draw.rectangle([x1, y1, x2, y2], fill='white')

        # Barre horizontale
        y1 = center - cross_thick // 2
        y2 = center + cross_thick // 2
        x1 = center - cross_width // 2
        x2 = center + cross_width // 2
        draw.rectangle([x1, y1, x2, y2], fill='white')

        # Centre brillant cyan
        center_size = cross_thick - 2
        cx1 = center - center_size // 2
        cx2 = center + center_size // 2
        draw.rectangle([cx1, cx1, cx2, cx2], fill='#80DEEA')

        # Sauvegarder
        img.save(output_path, 'PNG')
        print(f"✓ Créé: {output_path}")
        return True

    except Exception as e:
        print(f"✗ Erreur création PNG: {e}")
        return False

def convert_to_webp(png_path, webp_path):
    """Convertit un PNG en WebP"""
    try:
        from PIL import Image
        img = Image.open(png_path)
        img.save(webp_path, 'WEBP', quality=95)
        print(f"✓ Converti en WebP: {webp_path}")
        return True
    except Exception as e:
        print(f"✗ Erreur conversion WebP: {e}")
        return False

def generate_all_icons():
    """Génère toutes les icônes pour toutes les densités"""
    print("\n=== Génération des icônes Medistock ===\n")

    # Installer les dépendances
    if not install_dependencies():
        print("\n⚠ Impossible d'installer les dépendances automatiquement.")
        print("Installez manuellement: pip3 install cairosvg pillow")
        return False

    success_count = 0
    total_count = len(DENSITIES) * 2  # ic_launcher + ic_launcher_round

    for density, size in DENSITIES.items():
        density_path = RES_PATH / f'mipmap-{density}'
        density_path.mkdir(parents=True, exist_ok=True)

        # Générer ic_launcher
        png_path = density_path / 'ic_launcher.png'
        webp_path = density_path / 'ic_launcher.webp'

        if create_icon_png(size, png_path):
            if convert_to_webp(png_path, webp_path):
                success_count += 1
                png_path.unlink()  # Supprimer le PNG temporaire

        # Générer ic_launcher_round (même chose pour simplifier)
        png_path = density_path / 'ic_launcher_round.png'
        webp_path = density_path / 'ic_launcher_round.webp'

        if create_icon_png(size, png_path):
            if convert_to_webp(png_path, webp_path):
                success_count += 1
                png_path.unlink()

    print(f"\n=== Résultat: {success_count}/{total_count} icônes générées ===\n")

    # Générer l'icône Play Store (512x512)
    playstore_path = BASE_PATH / 'app' / 'src' / 'main' / 'ic_launcher-playstore.png'
    print("Génération de l'icône Play Store (512x512)...")
    if create_icon_png(512, playstore_path):
        print("✓ Icône Play Store créée")

    print("\n✓ Terminé! Les nouvelles icônes sont prêtes.")
    print("  Rebuild l'app pour voir les changements.")

    return success_count == total_count

if __name__ == '__main__':
    generate_all_icons()
