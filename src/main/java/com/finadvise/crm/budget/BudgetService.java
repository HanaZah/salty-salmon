package com.finadvise.crm.budget;

import com.finadvise.crm.clients.ClientRepository;
import com.finadvise.crm.common.MissingVersionException;
import com.finadvise.crm.common.OwnershipValidator;
import com.finadvise.crm.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {
    private final BudgetRepository budgetRepository;
    private final IncomeRepository incomeRepository;
    private final IncomeTypeRepository incomeTypeRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseTypeRepository expenseTypeRepository;
    private final ClientRepository clientRepository;
    private final BudgetMapper budgetMapper;
    private final OwnershipValidator ownershipValidator;

    @Transactional(readOnly = true)
    public BudgetFullDTO getBudget(Long clientId, String requesterEmployeeId) throws AccessDeniedException {
        if(!ownershipValidator.canAccessClient(clientId, requesterEmployeeId)) {
            throw new AccessDeniedException("Assigned advisor mismatch for client budget access");
        }

        List<BudgetItemDTO> incomes = incomeRepository.findAllByClientId(clientId).stream()
                .map(budgetMapper::toDto)
                .toList();
        List<BudgetItemDTO> expenses = expenseRepository.findAllByClientId(clientId).stream()
                .map(budgetMapper::toDto)
                .toList();
        BudgetSummaryProjection summary = budgetRepository.getSummaryByClientId(clientId);

        return new BudgetFullDTO(
                summary != null ? summary.getTotalIncomes() : BigDecimal.ZERO,
                summary != null ? summary.getTotalExpenses() : BigDecimal.ZERO,
                summary != null ? summary.getNetCashflow() : BigDecimal.ZERO,
                incomes,
                expenses
        );
    }

    @Transactional
    public void updateFullBudget(Long clientId, BudgetFullDTO dto, String requesterId) {
        if (!ownershipValidator.canAccessClient(clientId, requesterId)) {
            throw new AccessDeniedException("Unauthorized budget update attempt.");
        }

        List<Income> currentIncomes = incomeRepository.findAllByClientId(clientId);
        List<Expense> currentExpenses = expenseRepository.findAllByClientId(clientId);
        validateVersions(dto.incomes(), currentIncomes, Income.class);
        validateVersions(dto.expenses(), currentExpenses, Expense.class);
        applyIncomeUpdates(clientId, dto.incomes(), currentIncomes);
        applyExpenseUpdates(clientId, dto.expenses(), currentExpenses);

        // Summary values are naturally updated by the DB/JPA flush
    }

    private Long getIdFromEntity(Object entity) {
        if (entity instanceof Income i) return i.getId();
        if (entity instanceof Expense e) return e.getId();
        throw new IllegalArgumentException("Unknown budget entity type");
    }

    private Integer getVersionFromEntity(Object entity) {
        if (entity instanceof Income i) return i.getVersion();
        if (entity instanceof Expense e) return e.getVersion();
        throw new IllegalArgumentException("Unknown budget entity type");
    }

    private <T> void validateVersions(List<BudgetItemDTO> dtos, List<T> entities, Class<T> type) {
        if (dtos == null || dtos.isEmpty()) return;
        Map<Long, T> entityMap = entities.stream()
                .collect(Collectors.toMap(this::getIdFromEntity, Function.identity()));

        for (BudgetItemDTO dto : dtos) {
            if (dto.id() == null) continue;

            T entity = entityMap.get(dto.id());

            if (entity == null) {
                throw new ResourceNotFoundException(type.getSimpleName() + " ID " + dto.id() + " not found");
            }

            if (dto.version() == null) {
                throw new MissingVersionException("Version must be provided for existing budget item updates.");
            }

            // Compare versions using a helper that handles the type-specific access
            if (!Objects.equals(getVersionFromEntity(entity), dto.version())) {
                throw new ObjectOptimisticLockingFailureException(type, dto.id());
            }
        }
    }

    private void applyIncomeUpdates(Long clientId, List<BudgetItemDTO> dtos, List<Income> currentIncomes) {
        Map<Long, Income> incomeMap = currentIncomes.stream()
                .collect(Collectors.toMap(Income::getId, Function.identity()));

        for (BudgetItemDTO dto : dtos) {
            if (dto.id() != null) {
                Income existing = incomeMap.get(dto.id());
                if (existing != null) {
                    if (dto.amount() > 0) {
                        existing.setAmount(dto.amount());
                    } else {
                        incomeRepository.delete(existing);
                    }
                }
            } else if (dto.amount() > 0) {
                createNewIncome(clientId, dto);
            }
        }
    }

    private void applyExpenseUpdates(Long clientId, List<BudgetItemDTO> dtos, List<Expense> currentExpenses) {
        Map<Long, Expense> expenseMap = currentExpenses.stream()
                .collect(Collectors.toMap(Expense::getId, Function.identity()));

        for (BudgetItemDTO dto : dtos) {
            if (dto.id() != null) {
                Expense existing = expenseMap.get(dto.id());
                if (existing != null) {
                    if (dto.amount() > 0) {
                        existing.setAmount(dto.amount());
                    } else {
                        expenseRepository.delete(existing);
                    }
                }
            } else if (dto.amount() > 0) {
                createNewExpense(clientId, dto);
            }
        }
    }

    private void createNewIncome(Long clientId, BudgetItemDTO dto) {
        IncomeType type = incomeTypeRepository.findById(dto.typeId())
                .orElseThrow(() -> new ResourceNotFoundException("Income Type ID not found: " + dto.typeId()));

        Income newIncome = Income.builder()
                .amount(dto.amount())
                .incomeType(type)
                .client(clientRepository.getReferenceById(clientId)) // creates a lazy proxy, avoiding a SELECT query
                .build();

        incomeRepository.save(newIncome);
    }

    private void createNewExpense(Long clientId, BudgetItemDTO dto) {
        ExpenseType type = expenseTypeRepository.findById(dto.typeId())
                .orElseThrow(() -> new ResourceNotFoundException("Expense Type ID not found: " + dto.typeId()));

        boolean mandatoryFlag = Boolean.TRUE.equals(dto.isMandatory());  //null-safe flag extraction

        Expense newExpense = Expense.builder()
                .amount(dto.amount())
                .isMandatory(mandatoryFlag)
                .expenseType(type)
                .client(clientRepository.getReferenceById(clientId))
                .build();

        expenseRepository.save(newExpense);
    }
}
