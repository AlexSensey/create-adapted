import { DefaultTheme } from "vitepress";

export default {
  "/": [
    {
      text: "Getting Started",
      collapsed: false,
      items: [
        {
          text: "Adding Ponder to your project",
          link: "/guide/project-setup",
        },
        {
          text: "Registering a Ponder Plugin",
          link: "/guide/registering-ponder-plugin",
        },
        {
          text: "Creating your first ponder",
          link: "/guide/creating-your-first-ponder",
        },
      ],
    },
  ],
} as DefaultTheme.SidebarMulti;
