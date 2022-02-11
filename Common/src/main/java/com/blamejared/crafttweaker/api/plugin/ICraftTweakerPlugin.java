package com.blamejared.crafttweaker.api.plugin;

public interface ICraftTweakerPlugin {
    
    default void registerLoaders(final ILoaderRegistrationHandler handler) {}
    
    default void registerLoadSource(final IScriptLoadSourceRegistrationHandler handler) {}
    
    default void registerBracketParsers(final IBracketParserRegistrationHandler handler) {}
    
    default void registerRecipeHandlers(final IRecipeHandlerRegistrationHandler handler) {}
    
    default void manageJavaNativeIntegration(final IJavaNativeIntegrationRegistrationHandler handler) {}
    
    default void registerListeners(final IListenerRegistrationHandler handler) {}
    
}
