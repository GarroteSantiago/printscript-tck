{
  description = "SantiagoGarrote's Java Development Environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs =
    { nixpkgs, flake-utils, ... }:
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = nixpkgs.legacyPackages.${system};

        java = pkgs.javaPackages.compiler.openjdk21;

        packages = with pkgs; [
          # Java
          java

          # Build
          (gradle.override {
            inherit java;
          })

          # Java tooling
          jdt-language-server
          groovy-language-server

          # Development utilities
          just
        ];
      in
      {
        devShells.default = pkgs.mkShell {
          inherit packages;
        };
      }
    );
}
