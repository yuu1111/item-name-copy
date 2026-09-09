package com.github.yuu1111.itemnamecopy.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.minecraft.client.gui.screens.Screen;
//? if forge && <1.17 {
/*import net.minecraft.item.ItemStack;
*///?} else {
import net.minecraft.world.item.ItemStack;
//?}

final class RecipeViewerAccess {
    private static final ViewerAdapter EMI = EmiAdapter.create();
    private static final ViewerAdapter REI = ReiAdapter.create();
    private static final ViewerAdapter JEI = JeiAdapter.create();

    private RecipeViewerAccess() {
    }

    static boolean supports(Screen screen) {
        return EMI.supports(screen) || REI.supports(screen) || JEI.supports(screen);
    }

    static boolean isSearchFocused() {
        return EMI.isSearchFocused() || REI.isSearchFocused() || JEI.isSearchFocused();
    }

    static Optional<String> hoveredItemName(Screen screen) {
        Optional<String> name = EMI.hoveredItemName(screen);
        if (name.isPresent()) return name;
        name = REI.hoveredItemName(screen);
        return name.isPresent() ? name : JEI.hoveredItemName(screen);
    }

    private static final class JeiAdapter implements ViewerAdapter {
        private final Method getRuntime;
        private final Method runtimeGetIngredientListOverlay;
        private final Method runtimeGetBookmarkOverlay;
        private final Method runtimeGetRecipesGui;
        private final Method overlayGetIngredientUnderMouse;
        private final Method overlayHasKeyboardFocus;
        private final Method bookmarkGetIngredientUnderMouse;
        private final Method typedGetIngredient;
        private final Method recipesGetIngredientUnderMouse;
        private final Object itemStackType;

        private JeiAdapter(Method getRuntime, Method runtimeGetIngredientListOverlay,
                           Method runtimeGetBookmarkOverlay, Method runtimeGetRecipesGui,
                           Method overlayGetIngredientUnderMouse, Method overlayHasKeyboardFocus,
                           Method bookmarkGetIngredientUnderMouse, Method typedGetIngredient,
                           Method recipesGetIngredientUnderMouse, Object itemStackType) {
            this.getRuntime = getRuntime;
            this.runtimeGetIngredientListOverlay = runtimeGetIngredientListOverlay;
            this.runtimeGetBookmarkOverlay = runtimeGetBookmarkOverlay;
            this.runtimeGetRecipesGui = runtimeGetRecipesGui;
            this.overlayGetIngredientUnderMouse = overlayGetIngredientUnderMouse;
            this.overlayHasKeyboardFocus = overlayHasKeyboardFocus;
            this.bookmarkGetIngredientUnderMouse = bookmarkGetIngredientUnderMouse;
            this.typedGetIngredient = typedGetIngredient;
            this.recipesGetIngredientUnderMouse = recipesGetIngredientUnderMouse;
            this.itemStackType = itemStackType;
        }

        static ViewerAdapter create() {
            try {
                ClassLoader loader = RecipeViewerAccess.class.getClassLoader();
                Class<?> internal = Class.forName("mezz.jei.common.Internal", false, loader);
                Class<?> runtime = Class.forName("mezz.jei.api.runtime.IJeiRuntime", false, loader);
                Class<?> ingredientList = Class.forName(
                        "mezz.jei.api.runtime.IIngredientListOverlay", false, loader);
                Class<?> bookmark = Class.forName("mezz.jei.api.runtime.IBookmarkOverlay", false, loader);
                Class<?> typed = Class.forName("mezz.jei.api.ingredients.ITypedIngredient", false, loader);
                Class<?> recipes = Class.forName("mezz.jei.api.runtime.IRecipesGui", false, loader);
                Class<?> ingredientType = Class.forName("mezz.jei.api.ingredients.IIngredientType", false, loader);
                Class<?> vanillaTypes = Class.forName("mezz.jei.api.constants.VanillaTypes", false, loader);
                Method getRuntime;
                try {
                    getRuntime = internal.getMethod("getOptionalJeiRuntime");
                } catch (NoSuchMethodException exception) {
                    getRuntime = internal.getMethod("getRuntime");
                }
                return new JeiAdapter(
                        getRuntime, runtime.getMethod("getIngredientListOverlay"),
                        runtime.getMethod("getBookmarkOverlay"), runtime.getMethod("getRecipesGui"),
                        ingredientList.getMethod("getIngredientUnderMouse"),
                        ingredientList.getMethod("hasKeyboardFocus"),
                        bookmark.getMethod("getIngredientUnderMouse"), typed.getMethod("getIngredient"),
                        recipes.getMethod("getIngredientUnderMouse", ingredientType),
                        vanillaTypes.getField("ITEM_STACK").get(null)
                );
            } catch (ReflectiveOperationException | LinkageError exception) {
                return MissingAdapter.INSTANCE;
            }
        }

        @Override
        public boolean supports(Screen screen) {
            return true;
        }

        @Override
        public boolean isSearchFocused() {
            try {
                Object runtime = runtime();
                if (runtime == null) return false;
                Object overlay = runtimeGetIngredientListOverlay.invoke(runtime);
                return Boolean.TRUE.equals(overlayHasKeyboardFocus.invoke(overlay));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                return false;
            }
        }

        @Override
        public Optional<String> hoveredItemName(Screen screen) {
            try {
                Object runtime = runtime();
                if (runtime == null) return Optional.empty();

                Object overlay = runtimeGetIngredientListOverlay.invoke(runtime);
                Optional<String> name = typedIngredientName(overlayGetIngredientUnderMouse.invoke(overlay));
                if (name.isPresent()) return name;

                Object bookmark = runtimeGetBookmarkOverlay.invoke(runtime);
                name = typedIngredientName(bookmarkGetIngredientUnderMouse.invoke(bookmark));
                if (name.isPresent()) return name;

                Object recipes = runtimeGetRecipesGui.invoke(runtime);
                return itemName(optionalValue(recipesGetIngredientUnderMouse.invoke(recipes, itemStackType)));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                return Optional.empty();
            }
        }

        private Object runtime() throws ReflectiveOperationException {
            return optionalValue(getRuntime.invoke(null));
        }

        private Optional<String> typedIngredientName(Object result) throws ReflectiveOperationException {
            Object typed = optionalValue(result);
            return typed == null ? Optional.empty() : itemName(typedGetIngredient.invoke(typed));
        }

        private Object optionalValue(Object value) {
            if (!(value instanceof Optional)) return value;
            Optional<?> optional = (Optional<?>) value;
            return optional.isPresent() ? optional.get() : null;
        }
    }

    private interface ViewerAdapter {
        boolean supports(Screen screen);

        boolean isSearchFocused();

        Optional<String> hoveredItemName(Screen screen);
    }

    private static final class MissingAdapter implements ViewerAdapter {
        private static final MissingAdapter INSTANCE = new MissingAdapter();

        @Override
        public boolean supports(Screen screen) {
            return false;
        }

        @Override
        public boolean isSearchFocused() {
            return false;
        }

        @Override
        public Optional<String> hoveredItemName(Screen screen) {
            return Optional.empty();
        }
    }

    private static final class EmiAdapter implements ViewerAdapter {
        private final Method getHoveredStack;
        private final Method isSearchFocused;
        private final Method interactionIsEmpty;
        private final Method interactionGetStack;
        private final Method ingredientGetStacks;
        private final Method stackGetItemStack;

        private EmiAdapter(Method getHoveredStack, Method isSearchFocused, Method interactionIsEmpty,
                           Method interactionGetStack, Method ingredientGetStacks, Method stackGetItemStack) {
            this.getHoveredStack = getHoveredStack;
            this.isSearchFocused = isSearchFocused;
            this.interactionIsEmpty = interactionIsEmpty;
            this.interactionGetStack = interactionGetStack;
            this.ingredientGetStacks = ingredientGetStacks;
            this.stackGetItemStack = stackGetItemStack;
        }

        static ViewerAdapter create() {
            try {
                ClassLoader loader = RecipeViewerAccess.class.getClassLoader();
                Class<?> api = Class.forName("dev.emi.emi.api.EmiApi", false, loader);
                Class<?> interaction = Class.forName("dev.emi.emi.api.stack.EmiStackInteraction", false, loader);
                Class<?> ingredient = Class.forName("dev.emi.emi.api.stack.EmiIngredient", false, loader);
                Class<?> stack = Class.forName("dev.emi.emi.api.stack.EmiStack", false, loader);
                return new EmiAdapter(
                        api.getMethod("getHoveredStack", boolean.class), api.getMethod("isSearchFocused"),
                        interaction.getMethod("isEmpty"), interaction.getMethod("getStack"),
                        ingredient.getMethod("getEmiStacks"), stack.getMethod("getItemStack")
                );
            } catch (ReflectiveOperationException | LinkageError exception) {
                return MissingAdapter.INSTANCE;
            }
        }

        @Override
        public boolean supports(Screen screen) {
            return true;
        }

        @Override
        public boolean isSearchFocused() {
            try {
                return Boolean.TRUE.equals(isSearchFocused.invoke(null));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                return false;
            }
        }

        @Override
        public Optional<String> hoveredItemName(Screen screen) {
            try {
                Object interaction = getHoveredStack.invoke(null, false);
                if (interaction == null || Boolean.TRUE.equals(interactionIsEmpty.invoke(interaction))) {
                    return Optional.empty();
                }

                Object ingredient = interactionGetStack.invoke(interaction);
                List<?> stacks = (List<?>) ingredientGetStacks.invoke(ingredient);
                if (stacks.isEmpty()) return Optional.empty();

                int index = ingredient.getClass().getName().endsWith("ListEmiIngredient")
                        ? (int) (System.currentTimeMillis() / 1000 % stacks.size()) : 0;
                return itemName(stackGetItemStack.invoke(stacks.get(index)));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                return Optional.empty();
            }
        }
    }

    private static final class ReiAdapter implements ViewerAdapter {
        private final Class<?> slotClass;
        private final Method screenRegistryGetInstance;
        private final Method screenRegistryGetFocusedStack;
        private final Method pointOfMouse;
        private final Field pointX;
        private final Field pointY;
        private final Method slotContainsMouse;
        private final Method slotGetCurrentEntry;
        private final Method entryIsEmpty;
        private final Method entryGetValue;
        private final Method runtimeGetInstance;
        private final Method runtimeGetOverlay;
        private final Method runtimeGetSearchTextField;
        private final Method overlayGetEntryList;
        private final Method overlayGetFavoritesList;
        private final Method overlayListGetFocusedStack;

        private ReiAdapter(Class<?> slotClass, Method screenRegistryGetInstance,
                           Method screenRegistryGetFocusedStack, Method pointOfMouse, Field pointX, Field pointY,
                           Method slotContainsMouse, Method slotGetCurrentEntry, Method entryIsEmpty,
                           Method entryGetValue, Method runtimeGetInstance, Method runtimeGetOverlay,
                           Method runtimeGetSearchTextField, Method overlayGetEntryList,
                           Method overlayGetFavoritesList, Method overlayListGetFocusedStack) {
            this.slotClass = slotClass;
            this.screenRegistryGetInstance = screenRegistryGetInstance;
            this.screenRegistryGetFocusedStack = screenRegistryGetFocusedStack;
            this.pointOfMouse = pointOfMouse;
            this.pointX = pointX;
            this.pointY = pointY;
            this.slotContainsMouse = slotContainsMouse;
            this.slotGetCurrentEntry = slotGetCurrentEntry;
            this.entryIsEmpty = entryIsEmpty;
            this.entryGetValue = entryGetValue;
            this.runtimeGetInstance = runtimeGetInstance;
            this.runtimeGetOverlay = runtimeGetOverlay;
            this.runtimeGetSearchTextField = runtimeGetSearchTextField;
            this.overlayGetEntryList = overlayGetEntryList;
            this.overlayGetFavoritesList = overlayGetFavoritesList;
            this.overlayListGetFocusedStack = overlayListGetFocusedStack;
        }

        static ViewerAdapter create() {
            try {
                ClassLoader loader = RecipeViewerAccess.class.getClassLoader();
                Class<?> registry = Class.forName(
                        "me.shedaniel.rei.api.client.registry.screen.ScreenRegistry", false, loader);
                Class<?> point = Class.forName("me.shedaniel.math.Point", false, loader);
                Class<?> pointHelper = Class.forName("me.shedaniel.math.impl.PointHelper", false, loader);
                Class<?> slot = Class.forName("me.shedaniel.rei.api.client.gui.widgets.Slot", false, loader);
                Class<?> entry = Class.forName("me.shedaniel.rei.api.common.entry.EntryStack", false, loader);
                Class<?> runtime = Class.forName("me.shedaniel.rei.api.client.REIRuntime", false, loader);
                Class<?> overlay = Class.forName("me.shedaniel.rei.api.client.overlay.ScreenOverlay", false, loader);
                Class<?> overlayList = Class.forName(
                        "me.shedaniel.rei.api.client.overlay.OverlayListWidget", false, loader);
                return new ReiAdapter(
                        slot, registry.getMethod("getInstance"), registry.getMethod("getFocusedStack", Screen.class, point),
                        pointHelper.getMethod("ofMouse"), point.getField("x"), point.getField("y"),
                        slot.getMethod("containsMouse", double.class, double.class), slot.getMethod("getCurrentEntry"),
                        entry.getMethod("isEmpty"), entry.getMethod("getValue"), runtime.getMethod("getInstance"),
                        runtime.getMethod("getOverlay"), runtime.getMethod("getSearchTextField"),
                        overlay.getMethod("getEntryList"), overlay.getMethod("getFavoritesList"),
                        overlayList.getMethod("getFocusedStack")
                );
            } catch (ReflectiveOperationException | LinkageError exception) {
                return MissingAdapter.INSTANCE;
            }
        }

        @Override
        public boolean supports(Screen screen) {
            return true;
        }

        @Override
        public boolean isSearchFocused() {
            try {
                Object runtime = runtimeGetInstance.invoke(null);
                Object search = runtimeGetSearchTextField.invoke(runtime);
                return search != null && Boolean.TRUE.equals(search.getClass().getMethod("isFocused").invoke(search));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                return false;
            }
        }

        @Override
        public Optional<String> hoveredItemName(Screen screen) {
            try {
                Object mouse = pointOfMouse.invoke(null);
                Object registry = screenRegistryGetInstance.invoke(null);
                Optional<String> name = entryName(screenRegistryGetFocusedStack.invoke(registry, screen, mouse));
                if (name.isPresent()) return name;

                double mouseX = ((Number) pointX.get(mouse)).doubleValue();
                double mouseY = ((Number) pointY.get(mouse)).doubleValue();
                Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
                name = findHoveredSlot(screen, mouseX, mouseY, visited, 0);
                if (name.isPresent()) return name;

                Object runtime = runtimeGetInstance.invoke(null);
                Optional<?> overlay = (Optional<?>) runtimeGetOverlay.invoke(runtime);
                if (!overlay.isPresent()) return Optional.empty();

                Object entryList = overlayGetEntryList.invoke(overlay.get());
                name = entryName(overlayListGetFocusedStack.invoke(entryList));
                if (name.isPresent()) return name;

                Optional<?> favorites = (Optional<?>) overlayGetFavoritesList.invoke(overlay.get());
                return favorites.isPresent()
                        ? entryName(overlayListGetFocusedStack.invoke(favorites.get())) : Optional.empty();
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                return Optional.empty();
            }
        }

        private Optional<String> findHoveredSlot(Object listener, double mouseX, double mouseY,
                                                 Set<Object> visited, int depth) throws ReflectiveOperationException {
            if (listener == null || depth > 12 || !visited.add(listener)) return Optional.empty();
            if (slotClass.isInstance(listener)
                    && Boolean.TRUE.equals(slotContainsMouse.invoke(listener, mouseX, mouseY))) {
                Optional<String> name = entryName(slotGetCurrentEntry.invoke(listener));
                if (name.isPresent()) return name;
            }

            Method children;
            try {
                children = listener.getClass().getMethod("children");
            } catch (NoSuchMethodException exception) {
                return Optional.empty();
            }
            Object result = children.invoke(listener);
            if (!(result instanceof Iterable)) return Optional.empty();
            for (Object child : (Iterable<?>) result) {
                Optional<String> name = findHoveredSlot(child, mouseX, mouseY, visited, depth + 1);
                if (name.isPresent()) return name;
            }
            return Optional.empty();
        }

        private Optional<String> entryName(Object entry) throws ReflectiveOperationException {
            if (entry == null || Boolean.TRUE.equals(entryIsEmpty.invoke(entry))) return Optional.empty();
            return itemName(entryGetValue.invoke(entry));
        }
    }

    private static Optional<String> itemName(Object value) {
        if (!(value instanceof ItemStack)) return Optional.empty();
        ItemStack stack = (ItemStack) value;
        return stack.isEmpty() ? Optional.empty() : Optional.of(stack.getHoverName().getString());
    }
}
