// noinspection JSUnusedGlobalSymbols

import { defineConfig } from "vitepress";
import main from "./sidebars/main";

// https://vitepress.dev/reference/site-config
export default defineConfig({
  title: "Ponder Wiki",
  description: "Visual in-Game Documentation",

  cleanUrls: true,
  lastUpdated: true,

  srcDir: "src",
  srcExclude: ["**/README.md"],

  head: [["link", { rel: "icon", href: "/assets/ponder-icon.webp" }]],

  // https://vitepress.dev/reference/default-theme-config
  themeConfig: {
    logo: {
      src: "/assets/ponder-icon.webp",
      width: 24,
      height: 24,
    },

    // Switch to algolia once deployed
    search: { provider: "local" },

    nav: [
      { text: "Guide", link: "/guide" },
      { text: "API Docs", link: "/api-docs" },
    ],

    sidebar: {
      ...main,
    },

    socialLinks: [
      {
        icon: "github",
        link: "https://github.com/Creators-of-Create/Ponder",
      },
      { icon: "discord", link: "https://r.createmod.net/d" },
    ],

    editLink: {
      pattern:
        "https://github.com/Creators-of-Create/Ponder/edit/main/docs/:path",
      text: "Edit this page on GitHub",
    },
  },

  sitemap: {
    hostname: "https://ponder.createmod.net",
  },
});
